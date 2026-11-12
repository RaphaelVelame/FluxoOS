# Correção: reativação de conta não recupera sessões antigas

Implementada em 11/11/2026. O comportamento foi reproduzido antes da alteração e corrigido com regressões permanentes.

## Pesquisa e escolha

Foram pesquisadas implementações no GitHub, documentação oficial, discussões no Stack Overflow e no Reddit. As discussões foram usadas para comparar abordagens, e a decisão foi sustentada pelas fontes primárias e pelos testes locais. Não foi copiada uma configuração de `HttpSession` para o projeto, que usa seu próprio cookie opaco e `SessaoService`.

Referências consultadas:

- [OWASP — gestão de sessão](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet): invalidação no servidor e mudanças de privilégio.
- [Spring Security — implementação de SessionInformation no GitHub](https://github.com/spring-projects/spring-security/blob/main/core/src/main/java/org/springframework/security/core/session/SessionInformation.java): expiração de sessões registradas.
- [ASP.NET Core — SecurityStampValidator no GitHub](https://github.com/dotnet/aspnetcore/blob/main/src/Identity/Core/src/SecurityStampValidator.cs): rejeição de identidades cujo marcador de segurança não corresponde ao da conta.
- [Microsoft — reagir a alterações no backend](https://learn.microsoft.com/en-us/aspnet/core/security/authentication/cookie?view=aspnetcore-10.0#react-to-back-end-changes): verificar mudanças da conta durante a validação do cookie.
- [Stack Overflow — invalidação de sessão](https://stackoverflow.com/questions/15265554/spring-security-session-invalidate-vs-invalidate-session-true): comparação dos mecanismos próprios de HttpSession e Spring Security; não equivale à implementação de sessão desta aplicação.
- [Discussão no Reddit sobre invalidação de sessões](https://www.reddit.com/r/javahelp/comments/joshqf/): alternativa de marcar sessões por usuário. A consulta usou o conteúdo indexado; a página completa não foi recuperada.

Foram comparadas duas soluções:

| Abordagem | Avaliação para este projeto |
| --- | --- |
| Percorrer e revogar as sessões conhecidas de um usuário | Exige coordenar a emissão concorrente e o commit; percorrer somente o mapa local não alcança outros armazenamentos. Revogar antes do commit também pode encerrar sessões quando a alteração é revertida. |
| Versão de segurança persistida por conta e validada a cada requisição | Aproveita a consulta ao usuário já existente, cobre emissão com estado antigo e funciona com mapas independentes que consultem o mesmo banco. A mudança participa da transação. Foi a opção escolhida. |

A implementação usa uma versão numérica em vez de horário, evitando depender da precisão ou sincronização dos relógios. É uma adaptação do padrão de marcador de segurança, não uma migração para ASP.NET ou uma troca da autenticação atual.

## Comportamento implementado

1. A migração aditiva `V3__versao_sessao_usuario.sql` acrescenta `versao_sessao`, com zero para usuários existentes. V1 e V2 permanecem intactas.
2. A sessão registra a versão da conta observada durante a autenticação.
3. Desativar incrementa a versão e muda o status na mesma transação. Alterações de status bloqueiam a linha do usuário para serializar escritas concorrentes.
4. Reativar mantém a versão: nenhum cookie antigo recupera acesso. Repetir a ativação de uma conta já ativa não encerra sessões válidas.
5. A validação exige usuário ativo e versão correspondente, além das proteções anteriores. Um login iniciado com estado antigo não gera acesso válido depois da desativação/reativação.
6. Trocar a senha incrementa a versão na mesma atualização condicional do hash. Isso substitui a varredura do mapa e cobre emissão atrasada e rollback também nesse fluxo.
7. Logout individual continua revogando somente a sessão apresentada.

Essa invalidação passa a valer nas validações que observam a alteração confirmada no banco. Ela não cancela uma operação que já havia sido autorizada e estava em execução. Sessões continuam locais à JVM; implantação com balanceamento ainda precisa de armazenamento compartilhado ou estratégia de roteamento adequada. A [correção posterior de capacidade e descarte](CORRECAO_LIMITES_2026-11-11.md) acrescentou limite global de 20 sessões por JVM e limpeza automática, preservando as garantias de rollback.

## Validação executada

| Verificação | Resultado |
| --- | --- |
| Regressões antes da correção | Quatro testes executados: três reproduziram o defeito; rollback já preservava acesso |
| `clean verify` com JDK 25 | 33 testes passaram, nenhuma falha/erro/ignorado; JAR gerado |
| PostgreSQL 18.6 descartável | 20 testes passaram, incluindo operação, revogação e atualização V2 → V3 |
| Edge headless com três sessões independentes | Sete verificações passaram |
| Migração V2 → V3 | Conta, hash e status existentes preservados, nova versão inicial zero |
| Limpeza PostgreSQL | Esquemas de teste removidos; servidor temporário encerrado |

As regressões cobrem dois dispositivos, um dispositivo que não consulta durante a desativação, bloqueio de login de conta inativa, novo login depois da reativação, preservação de acesso de outras contas, novos ciclos de desativação, emissão atrasada coordenada entre threads, armazenamentos independentes, rollback da desativação, troca de senha e rollback da senha.

O ensaio de navegador usou a tela de chefia para desativar e reativar o técnico. A primeira sessão voltou à tela de login após atualizar; a segunda conservou seu cookie antigo sem consultar durante a desativação e recebeu 401 após reativar. Novo login funcionou, o cookie anterior continuou recusado, e CSRF/logout permaneceram funcionais.

As mudanças de código foram testadas em bancos descartáveis; o banco local do projeto não foi migrado ou alterado. Ao instalar o JAR atualizado, Flyway aplica a V3 no banco selecionado. A mudança de esquema é necessária para essa versão do código.

## Repetição e evidências

Configure JDK 25 em `JAVA_HOME` e `PATH`, então execute `mvnw clean verify`. O CI e `scripts/testar-postgres.ps1` agora incluem `FluxoOperacionalTest`, `RevogacaoSessaoTest` e `MigracaoSessaoTest` na etapa PostgreSQL. A execução local do script depende de psql e da política PowerShell; nesta máquina, a suíte PostgreSQL foi acionada diretamente pelo Maven/JDBC.

Evidências locais, ignoradas pelo Git:

- `target/logs/correcao-sessoes-antes.log`: reprodução anterior à correção.
- `target/logs/correcao-sessoes-final.log`: build limpo e 33 testes.
- `target/logs/correcao-sessoes-postgres.log`: 20 testes em PostgreSQL.
- `target/logs/correcao-sessoes-browser.log`: sete verificações no navegador.
- `target/correcao-sessoes/h2/` e `postgres/`: relatórios Surefire separados.
- `target/correcao-sessoes/browser-results.json`: resultado do navegador.

O teste de migração cria um esquema aleatório próprio e o remove em `finally`. Os testes operacionais continuam exigindo banco/esquema descartável: nunca apontar `TEST_DATABASE_URL` para dados reais.
