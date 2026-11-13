# Revisão final de código e testes — 12/11/2026

Revisão das fontes Java, interface JavaScript/CSS, migrations, importador PowerShell,
configuração, scripts de teste e workflow do GitHub Actions, incluindo as alterações pendentes
de importação, integridade e separação do estoque por proprietário. Validação local concluída
antes do commit. Os resultados abaixo descrevem o escopo verificado; não são garantia de
ausência de todo defeito possível.

## Defeitos reproduzidos e corrigidos

| Problema | Reprodução antes da correção | Comportamento final |
| --- | --- | --- |
| Limite de corpo HTTP dependia de `Content-Length` | JSON UTF-8 acima de 300.000 bytes enviado em blocos chegava ao parser e retornava 400, em vez de 413; falhou nos dois contextos de servidor | O filtro lê no máximo 300.001 bytes, rejeita excesso com 413 e reapresenta o corpo aceito ao controller. Corpo de exatamente 300.000 bytes, acentos, login, CSRF e criação continuam funcionando |
| Proprietário conhecido podia aparecer como desconhecido | Com `estoqueId` preenchido e resposta `/estoques` sem esse cadastro, o cartão mostrava “Sem classificação”; teste de navegador falhou | Mostra `Estoque #ID`, conta em “Estoque a confirmar”, preserva o filtro de NULL e não oferece classificação indevida |
| Erros HTTP eram tratados como falha interna | Consulta autenticada a rota inexistente retornava 500; teste de regressão falhou | Rota ausente: 404; método incompatível: 405 com `Allow`; corpo incompatível: 415; ID não numérico: 400. Mensagens não expõem classes nem stack trace |

As regressões do filtro e do tratamento HTTP são executadas com e sem contexto de servlet.
O teste de propriedade também verifica o contador, a ausência da ação de classificação,
o filtro “Sem classificação” e a recuperação após recarregar os cadastros.

## Dependências

Consulta OSV em 12/11/2026 (horário local), com 98 coordenadas Maven resolvidas, e `npm audit`
das dependências de testes de navegador. A consulta Maven inicial encontrou 22 associações
de versão a aviso; a consulta após as atualizações encontrou duas. Nenhum aviso foi retornado
pelo npm na consulta realizada.

| Componente | Antes | Depois |
| --- | --- | --- |
| Tomcat embarcado | 10.1.55 | 10.1.61 |
| Jackson BOM/core/databind | 2.21.4 | 2.21.8 |
| Log4j BOM/API | 2.24.3 | 2.25.5 |
| PostgreSQL JDBC | 42.7.11 | 42.7.14 |

As versões estão fixadas nas propriedades gerenciadas do POM; os testes finais usam esse
conjunto. Foram consultados os avisos oficiais de [Tomcat](https://tomcat.apache.org/security-10.html),
[Jackson](https://github.com/FasterXML/jackson-core/security/advisories/GHSA-p6pp-m3f8-5c89),
[Apache Logging](https://logging.apache.org/security.html) e
[pgJDBC](https://jdbc.postgresql.org/security/).

### Dois avisos que continuam no inventário

Ambos referem-se a `spring-webmvc:6.2.19`. Não foram suprimidos nem considerados corrigidos
na biblioteca. A análise das fontes/configuração não encontrou as condições de exploração
documentadas pelos fornecedores:

| Aviso | Condição necessária | Avaliação neste aplicativo |
| --- | --- | --- |
| [CVE-2026-47884](https://spring.io/security/cve-2026-47884/) | Uso de `XsltView` e resolução implícita de nome de view por mapeamento `/**` | Não há XSLT nem controller que renderize essas views. Os controllers da aplicação são REST; a interface é servida como arquivos estáticos |
| [CVE-2026-47890](https://spring.io/security/cve-2026-47890/) | Fragmentos de views enviados por Server-Sent Events com dados controlados pelo atacante | Não há SSE, `SseEmitter`, streaming de fragmentos nem endpoints `text/event-stream` |

Essa avaliação de não aplicabilidade depende da arquitetura atual e deve ser refeita ao
introduzir templates ou streaming. Segundo o fornecedor, a correção de biblioteca é 6.2.20
com suporte empresarial ou 7.0.9 na linha aberta. Não foi feita uma troca de versão principal
de Spring/Boot durante esta revisão. O relatório OSV usa classificação crítica para ambos;
o fornecedor classifica esses avisos como médio e baixo, respectivamente. Portanto, o
resultado não deve ser apresentado como “zero vulnerabilidades nas dependências”.

## Validação final

| Verificação | Resultado |
| --- | --- |
| Maven `verify`, Java 25, H2 em memória | 83 testes; zero falhas, erros ou ignorados; JAR executável gerado |
| PostgreSQL 18 em cluster novo e descartável | 52 testes; zero falhas, erros ou ignorados |
| Playwright em Microsoft Edge, com o JAR atualizado e H2 próprio | 23 testes; zero falhas ou ignorados |
| Importação sintética no PostgreSQL descartável | Extração, acentos, referências, saldos, códigos, rejeição de inválidos, preservação, reimportação e rollback integral aprovados |
| Sintaxe PowerShell | Todos os scripts em `scripts` e `tests` analisados sem erro |
| Sintaxe JavaScript | 10 arquivos verificados com `node --check`, sem erro |
| Whitespace Git | `git diff --check` aprovado |

Os testes operacionais incluem permissões, CSRF, revogação de sessão, idempotência,
concorrência de consumo, classificação de materiais, separação de saldos e preservação do
histórico. Os testes SQL exercitam restrições diretamente, legado inválido, recusa de
migrations, reversões e reaplicações sem limpar dados para fazê-las passar.

Comandos principais para repetição, com Java 25, Node 24, dependências de navegador e binários
locais do PostgreSQL disponíveis:

```powershell
.\mvnw.cmd -Dfluxoos.build.directory=target/revisao-final verify
.\scripts\testar-postgres.ps1 -IncluirImportacao
$env:FLUXOOS_TEST_JAR = (Resolve-Path target/revisao-final/fluxoos-1.0.0-SNAPSHOT.jar).Path
$env:BROWSER_CHANNEL = 'msedge'
node --test tests/e2e/gravacao.test.mjs
npm audit --prefix tests/e2e
git diff --check
```

Os logs locais desta execução ficam em `target/revisao-completa.log`, `target/revisao-postgres.log`,
`target/revisao-e2e.log`, `target/revisao-osv.json` e `target/revisao-npm-audit.json`, todos ignorados
pelo Git. O workflow executa Maven/H2, interface em Chromium e PostgreSQL descartável no GitHub;
seu resultado remoto é independente dos resultados locais registrados aqui.

## Dados e implantação

Nenhum teste desta revisão usou o banco operacional, reiniciou seu aplicativo ou aplicou as
migrations pendentes à base real. Os 169 materiais legados continuam aguardando identificação
do proprietário conforme decisão do usuário. A publicação do código no GitHub não implanta o
JAR nem atualiza o banco. As instruções de implantação e reversão estão nos documentos de
[integridade](MIGRACOES_INTEGRIDADE_2026-11-12.md) e [propriedade](ESTOQUES_POR_PROPRIETARIO.md).

`.env`, dados locais, senhas, ferramentas, dependências baixadas e artefatos de execução ficam
fora do commit. Permanecem os limites operacionais já descritos no README: sessões em memória,
necessidade de configurar HTTPS/backups no destino e ausência de fluxo de transferência de
propriedade de materiais já classificados.
