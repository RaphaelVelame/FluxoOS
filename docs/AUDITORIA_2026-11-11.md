# Auditoria de entrega — FluxoOS / os-inventory-app

Data: 11/11/2026, America/Bahia.

**Parecer: o fluxo principal funciona, mas ainda há correções necessárias antes de uma entrega de produção.** Compilar e passar na suíte atual não cobre as falhas exploratórias descritas abaixo. Não foi atribuído um percentual de prontidão: faltam critérios de aceite do cliente e validação do ambiente definitivo.

## Escopo e resultado executado

Revisados os 48 fontes Java principais, os cinco arquivos Java de testes, os oito módulos JavaScript, HTML, dois arquivos CSS, migrações, configurações, scripts e workflow. A árvore `src` tem 69 arquivos. A revisão considera o conteúdo local, incluindo alterações já existentes sem commit; o HEAD de referência é `d9973cedeb09e835ff2eaf0859c3b374ec2787c4`.

| Verificação | Resultado |
| --- | --- |
| Maven `clean verify` final, com JDK 25 | 17 testes passaram, nenhuma falha ou teste ignorado; JAR gerado |
| Operação em PostgreSQL 18.6 real e descartável | 12 testes passaram; concorrência, rollback, idempotência e assinatura incluídos |
| Flyway no PostgreSQL | V1 e V2 aplicadas com sucesso; esquema temporário removido ao final |
| Ensaios HTTP em Tomcat real | 26 verificações: 19 resultados esperados e sete divergências descritas abaixo |
| Caminho de contexto `/fluxo` | Duas consultas sem autenticação retornaram 200, quando deveriam ser protegidas |
| JavaScript | Oito módulos sem erros de sintaxe |
| Navegador Edge headless | 13 verificações funcionais: nove passaram e quatro expuseram falhas; nenhum erro JavaScript não tratado nos fluxos exercitados |
| Celular emulado, 360px | Navegação sem transbordamento horizontal nas cinco telas verificadas; conclusão com eventos de toque sintéticos passou |
| Acessibilidade automatizada, axe | Contraste insuficiente em 34 elementos do painel desktop e 11 da tela de conta móvel |
| Sintaxe PowerShell | Quatro scripts analisados, nenhum erro de sintaxe |
| Dependências Maven, OSV | 97 dependências consultadas; 22 correspondências com avisos de segurança em seis componentes |

O fluxo de navegador cobriu login, painel, cadastro de material, entrada de estoque, abertura de OS, atribuição, início, relato, consumo, assinatura e consulta da conclusão. Os ensaios usaram H2 em memória e um cluster PostgreSQL próprio, restritos a loopback. O banco em `data/` e as tabelas da instalação configurada no `.env` não foram utilizados nos testes. Nenhum código funcional foi corrigido nesta auditoria.

O Java padrão desta máquina é 17 e falhou com `release version 25 not supported`. Foi preparado JDK 25.0.5 portátil em `.tools`, sem mudar o Java global. Um build interrompido apresentou classes temporárias ausentes; a repetição de `verify` passou. Isso não foi classificado como erro de imports do código.

## Falhas reproduzidas e correções

### A01 — Alto: autenticação ignorada quando existe caminho de contexto

**Atualização após aprovação da correção:** o filtro passou a usar `getServletPath()` tanto para identificar a API quanto para liberar somente POST de login. A regressão demonstrou também o desvio por `/%61pi/usuarios`, corrigido pela mesma mudança. Os testes com Tomcat real passaram em `/` e `/fluxo`, incluindo sessão, assinatura protegida, CSRF e URLs codificadas/com ponto e vírgula. `mvnw verify` passou com 25 testes e gerou o JAR. A adaptação das URLs do frontend e do caminho do cookie para navegação completa em subcontexto continua pendente; os testes HTTP enviam o cookie explicitamente.

Origem: `src/main/java/br/com/fluxoos/config/ProtecaoHttp.java:58`.

O filtro protege apenas URIs iniciadas por `/api/`. Com `--server.servlet.context-path=/fluxo`, a URI passa a começar por `/fluxo/api/` e o filtro deixa a requisição seguir sem validar sessão.

| Requisição sem Cookie, na instância de contexto | Resultado |
| --- | --- |
| `GET /fluxo/api/usuarios` | 200 e dados da conta administrativa |
| `GET /fluxo/api/ordens` | 200 e listagem de ordens |

**Condição:** essa falha foi demonstrada com contexto configurado. Na configuração padrão `/`, as consultas sem sessão retornaram 401. O firewall também bloqueou as URLs com ponto e vírgula que motivaram a revisão anterior.

**Fazer:** aplicar autenticação ao caminho efetivo dentro do contexto e proteger a API por padrão; criar teste com servidor real e contexto não vazio. Ajustar também caminho do cookie e URLs absolutas da interface caso a implantação em subcaminho seja suportada. Até lá, impedir explicitamente essa configuração insegura.

**Aceite:** consultas e assinaturas exigem sessão tanto em `/` quanto em `/fluxo`; escrita exige sessão, autorização e CSRF nos dois cenários.

### A02 — Alto: reativação recupera sessões antigas

**Atualização após correção:** a versão de sessão da conta agora é persistida na migração V3, incrementada na desativação e na troca de senha, e comparada em toda validação. Reativar não restaura versões antigas. A regressão também cobre emissão atrasada de sessão, rollback e armazenamentos independentes. Passaram 33 testes no build limpo, 20 em PostgreSQL 18.6 e sete verificações no navegador. Pesquisa, decisões e evidências estão em [correção de sessões](CORRECAO_SESSOES_2026-11-11.md).

Origem: `service/UsuarioService.java:40` e `service/SessaoService.java:53`, sob `src/main/java/br/com/fluxoos/`.

Reproduzido: login do técnico → desativar a conta → mesmo cookie recebe 401 → reativar → mesmo cookie volta a receber 200, sem novo login. Desativar altera somente o usuário; não revoga suas sessões.

**Fazer:** revogar todas as sessões na desativação, com uma estratégia que também trate concorrência entre login e alteração de conta/senha. Reativação deve exigir nova autenticação. Acrescentar regressão para o ciclo completo, não apenas para a fase desativada.

### A03 — Alto: falha ao atualizar a tela pode gerar duas OS

**Atualização após correção:** formulários agora separam envio e confirmação da atualização da tela. A gravação confirmada fecha o modal e, se as consultas falharem, oferece recuperação por GET sem reenvio. A abertura de OS ganhou idempotência persistida pela migração V4, inclusive para requisições concorrentes. Os testes e a pesquisa estão em [correção da gravação e atualização](CORRECAO_GRAVACAO_2026-11-11.md).

Origem: `src/main/resources/static/js/views/orders.js:11`, `js/app.js:21` e `js/components.js:54`.

O POST cria a ordem, mas a confirmação da interface depende de quatro GETs posteriores. Foi simulada uma resposta 503 em `/api/usuarios` depois de o POST ter sido confirmado. O modal exibiu erro e permitiu repetir. A repetição criou **duas ordens com o mesmo conteúdo**.

**Fazer:** separar confirmação da gravação e atualização da tela; mostrar que a OS já foi criada mesmo quando a atualização falhar. Acrescentar idempotência no servidor para abertura de OS e preservar a chave durante retentativas. Revisar o mesmo padrão nos demais formulários: conclusão, cadastro e relato também podem ter sucesso no banco e parecer falhar ao usuário.

**Aceite:** perda de resposta ou falha de GET após POST não duplica OS e oferece recuperação da tela sem repetir a gravação.

### A04 — Alto para operação contínua: armazenamento de autenticação sem descarte

**Atualização após correção:** sessões têm limite global de 20 por JVM, admissão atômica e descarte no acesso, logout, novo login e manutenção periódica. Tentativas têm descarte por expiração, capacidade máxima e preservação de bloqueios ativos. A política passou para cinco erros por usuário em 15 minutos com bloqueio por cinco minutos, mais proteção por IP. Passaram 55 testes Java, 30 em PostgreSQL e 19 no navegador. Consulte [limites e descarte](CORRECAO_LIMITES_2026-11-11.md).

Origem: `repository/SessoesEmMemoria.java:13` e `seguranca/LimitadorTentativas.java:11`.

Sessões expiradas/revogadas e nomes/IPs do limitador permanecem em mapas sem limite de capacidade. Um ensaio criou 20 sessões e as revogou: no instante simulado de um dia depois, nenhuma era válida, mas as 20 continuavam armazenadas. Vinte nomes de login também continuaram no mapa depois da janela de bloqueio.

**Risco:** crescimento de heap, pausas de coleta de lixo e eventual indisponibilidade. A taxa de crescimento e o ponto de esgotamento não foram medidos. A auditoria de login no banco também exige retenção e controle de volume.

**Fazer:** expiração, descarte periódico e capacidade limitada, sem enfraquecer o bloqueio; métricas de tamanho. Para múltiplas instâncias, compartilhar sessão e rate limit. Medir ainda o custo de `AutenticacaoService.autenticar`, cujo `synchronized` serializa os logins durante PBKDF2 e consultas.

### A05 — Médio: limite de requisição contornado com envio em blocos

Origem: `config/ProtecaoHttp.java:63` e `src/main/resources/application.yml`.

O filtro mede somente `Content-Length`. O mesmo JSON de **310.064 bytes** retornou 413 com tamanho conhecido e 200 com `Transfer-Encoding: chunked`, que não informa esse tamanho. O parâmetro Tomcat de formulário não constitui limite geral para corpos JSON.

**Fazer:** limitar bytes efetivamente lidos e configurar limite coerente no proxy; manter a validação do campo de assinatura. Testar tamanho conhecido, chunked e leitura parcial. Este ensaio comprovou o desvio do limite, sem executar ataque de esgotamento de memória.

### A06 — Médio: erros do cliente viram 500

Origem: `controller/TratamentoErros.java:37`.

Com autenticação válida:

| Caso | Atual | Esperado |
| --- | --- | --- |
| `GET /api/ordens/abc` | 500 | 400 |
| `DELETE /api/materiais` | 500 | 405 |
| `GET /api/nao-existe` | 500 | 404 |
| POST de OS com `Content-Type: text/plain` | 500 | 415 |

**Fazer:** preservar os códigos de exceções HTTP conhecidas; manter mensagem segura para falhas internas. Registrar diagnóstico de erros inesperados com contexto e stack trace, evitando credenciais e assinaturas. Hoje o log registra apenas o nome da exceção, o que dificulta suporte.

### A07 — Médio: abas perdem sincronização de sessão e CSRF

Origem: `static/js/api.js:1` e inicialização de `static/js/app.js`.

O cookie é compartilhado, mas o token CSRF fica no `sessionStorage` de cada aba. Duas falhas foram reproduzidas: uma nova aba pede login apesar do cookie válido; depois de fazer login nela, uma escrita na primeira aba retorna 403 por CSRF antigo.

**Fazer:** definir recuperação/sincronização segura da sessão e CSRF entre abas, com tratamento claro de 403 e troca de usuário. Testar abas simultâneas, logout, expiração e troca de senha. Evitar resolver isso expondo o token de sessão em JavaScript.

### A08 — Médio: não há saída de conta visível no celular

Origem: `static/js/app.js:51`, `static/js/views/team.js:28` e `static/css/styles.css:240`.

O único botão de logout fica na sidebar, oculta abaixo de 900px. Em 360px, inclusive em Minha conta, não existe botão Sair visível. Isso dificulta encerrar acesso em aparelho compartilhado.

**Fazer:** oferecer logout em Minha conta ou no menu móvel; revogar sessão no servidor e sincronizar a saída nas outras abas.

### A09 — Condição de implantação: HTTPS por proxy pode impedir gravações/login

Origem: `config/ProtecaoHttp.java:68` e configuração de servidor.

Na simulação de HTTPS externo encaminhado por HTTP, login com `Origin: https://cliente.example` e cabeçalhos `X-Forwarded-*` retornou 403. O filtro compara a origem com o endereço percebido pelo backend; a configuração atual não demonstra tratamento confiável desses cabeçalhos.

**Fazer:** configurar o proxy real e a interpretação de cabeçalhos encaminhados, confiando somente no proxy autorizado. Validar cookies Secure, origem e IP usado no limitador. Não confiar indiscriminadamente em cabeçalhos enviados pelo cliente. A simulação não substitui um ensaio na infraestrutura definitiva.

### A10 — Médio, por inspeção: listagens crescem junto com toda a base

Origem: `service/OrdemServicoService.java:28`, `service/EstoqueService.java:37`, repositories e `static/js/app.js:21`.

Ordens, materiais, equipe, movimentos e eventos não têm paginação. A interface carrega quatro coleções completas em várias operações. A listagem de OS lê entidades com assinatura e percorre itens/materiais; o DTO omite a imagem na resposta, mas não impede sua leitura do banco. Esse caminho favorece consultas adicionais por OS.

**Fazer:** paginação e filtros no servidor, projeção resumida sem assinatura/itens completos, carregamento de detalhes sob demanda e atualização somente dos dados afetados. Medir consultas, payload, memória e latência com volume representativo. Não foi demonstrada lentidão na base pequena utilizada.

### A11 — Médio: contraste prejudica leitura

Origem: `static/css/tokens.css` e cores de texto em `static/css/styles.css`.

O axe encontrou uma categoria de violação, `color-contrast`, em 34 elementos do painel desktop e 11 de Minha conta móvel. Exemplos: texto de navegação com contraste 3,48:1 e rótulo da sidebar com 2,58:1, abaixo dos 4,5:1 esperados para esses textos. Há textos de apoio muito pequenos.

**Fazer:** escurecer textos secundários, revisar tamanhos, estados e foco; repetir axe e revisão manual de teclado, zoom, leitor de tela e aparelho físico. A emulação de toque e duas páginas analisadas não certificam acessibilidade completa.

### A12 — Médio para suporte: auditoria administrativa incompleta na consulta

Origem: `service/AuditoriaService.java:39` e `enums/TipoEventoSeguranca.java`.

Criação e alteração de status de contas são gravadas como `USUARIO_CRIADO` e `STATUS_USUARIO`, mas a consulta de segurança seleciona apenas tipos do enum; esses eventos ficam fora da tela. `ACESSO_NEGADO` está declarado sem emissão nos fluxos revisados. A tela mostra apenas os primeiros 100 eventos depois de receber a coleção inteira.

**Fazer:** disponibilizar eventos administrativos, autor/alvo, filtros e paginação; emitir recusas relevantes com controle de volume; definir retenção. A escolha de quais eventos são obrigatórios deve ser alinhada à operação do cliente.

### A13 — Baixo: aviso de sessão expirada em visita inicial

Origem: `static/js/api.js` e chamada inicial de `/auth/me` em `static/js/app.js`.

Uma visita sem cookie recebe 401, dispara o evento global de expiração e mostra o toast de sessão expirada. O aviso pode continuar visível após um login rápido, como apareceu na captura móvel.

**Fazer:** distinguir ausência inicial de sessão de expiração durante uso e limpar avisos antigos depois do login.

## Dependências: resultado e triagem necessária

A consulta OSV identificou **22 avisos por versão**. A classificação automática trouxe sete `CRITICAL`, sete `HIGH` e oito `MODERATE`, mas isso **não demonstra 22 falhas exploráveis na aplicação**. Há divergências com os fornecedores: o aviso Spring de SSE é classificado como baixo pelo Spring e depende de SSE com fragmentos; o de XsltView é médio e depende dessa configuração. Esses recursos não aparecem no código revisado. A página Apache também classifica o aviso de geração do web.xml efetivo como baixo. [Spring SSE](https://spring.io/security/cve-2026-47890/), [Spring XsltView](https://spring.io/security/cve-2026-47884/), [Apache Tomcat](https://tomcat.apache.org/security-10.html).

O pgJDBC exige uma condição específica de `channelBinding=require` com TLS para o aviso encontrado. O aviso Jackson de cache de type IDs exige polimorfismo com fallback; essa configuração não foi localizada nos DTOs revisados. A recomendação é atualizar e documentar a aplicabilidade, sem tratar a severidade automática como evidência de invasão. [pgJDBC](https://github.com/pgjdbc/pgjdbc/security/advisories/GHSA-j92g-9f8w-j867), [Jackson](https://github.com/FasterXML/jackson-databind/security/advisories/GHSA-wv8q-qhhj-9h54).

**Fazer:** atualizar o conjunto gerenciado pelo Spring Boot para uma combinação suportada, testar migrações e repetir o scan. Não inserir Spring Framework 7 isoladamente em Boot 3.5. O fornecedor lista correções Spring 6.2.20 sob suporte comercial e 7.0.9 em OSS para os dois avisos citados. As versões da tabela seguinte são correções dos avisos individuais, não uma recomendação de parar nelas: a página Apache contém avisos posteriores aos retornados nesta consulta OSV.

| Componente e versão presente | Aviso | Severidade OSV | Correção informada para o aviso |
| --- | --- | --- | --- |
| log4j-api 2.24.3 | [CVE-2026-49844](https://osv.dev/vulnerability/GHSA-qv9r-c865-cp47) | MODERATE | 2.25.5 / 2.26.1 |
| tomcat-embed-core 10.1.55 | [CVE-2026-55956](https://osv.dev/vulnerability/GHSA-5v3h-fjv2-54fg) | MODERATE | 10.1.56 |
| tomcat-embed-core 10.1.55 | [CVE-2026-65905](https://osv.dev/vulnerability/GHSA-9xv2-5v5q-p794) | CRITICAL | 10.1.58 |
| tomcat-embed-core 10.1.55 | [CVE-2026-65637](https://osv.dev/vulnerability/GHSA-f525-44xv-f2qj) | CRITICAL | 10.1.59 |
| tomcat-embed-core 10.1.55 | [CVE-2026-65182](https://osv.dev/vulnerability/GHSA-gcx9-497g-6cp6) | CRITICAL | 10.1.58 |
| tomcat-embed-core 10.1.55 | [CVE-2026-68525](https://osv.dev/vulnerability/GHSA-h3x4-894j-xpx5) | CRITICAL | 10.1.58 |
| tomcat-embed-core 10.1.55 | [CVE-2026-53404](https://osv.dev/vulnerability/GHSA-h792-v28v-ppgr) | HIGH | 10.1.56 |
| tomcat-embed-core 10.1.55 | [CVE-2026-55276](https://osv.dev/vulnerability/GHSA-mqg3-r7h5-24x4) | CRITICAL | 10.1.56 |
| spring-webmvc 6.2.19 | [CVE-2026-47890](https://osv.dev/vulnerability/GHSA-j9f9-w8pj-32f8) | CRITICAL | 7.0.9 OSS / 6.2.20 comercial |
| spring-webmvc 6.2.19 | [CVE-2026-47884](https://osv.dev/vulnerability/GHSA-pc63-qcmh-9cmg) | CRITICAL | 7.0.9 OSS / 6.2.20 comercial |
| jackson-databind 2.21.4 | [CVE-2026-59889](https://osv.dev/vulnerability/GHSA-5gvw-p9qm-jgwh) | MODERATE | 2.21.5 |
| jackson-databind 2.21.4 | [CVE-2026-54515](https://osv.dev/vulnerability/GHSA-5jmj-h7xm-6q6v) | MODERATE | 2.21.5 |
| jackson-databind 2.21.4 | [CVE-2026-91777](https://osv.dev/vulnerability/GHSA-cxp5-3px4-pw24) | HIGH | 2.21.7 |
| jackson-databind 2.21.4 | [CVE-2026-83557](https://osv.dev/vulnerability/GHSA-gx83-3vf8-gh7j) | MODERATE | 2.21.6 |
| jackson-databind 2.21.4 | [GHSA-mhm7-754m-9p8w](https://osv.dev/vulnerability/GHSA-mhm7-754m-9p8w) | MODERATE | 2.21.5 |
| jackson-databind 2.21.4 | [CVE-2026-68497](https://osv.dev/vulnerability/GHSA-q4xh-88c3-wmh7) | HIGH | 2.21.6 |
| jackson-databind 2.21.4 | [CVE-2026-77310](https://osv.dev/vulnerability/GHSA-vvgp-rfg2-7rr6) | MODERATE | 2.21.5 |
| jackson-databind 2.21.4 | [CVE-2026-19032](https://osv.dev/vulnerability/GHSA-wjgm-6hv5-3cvf) | MODERATE | 2.21.6 |
| jackson-databind 2.21.4 | [CVE-2026-91776](https://osv.dev/vulnerability/GHSA-wv8q-qhhj-9h54) | HIGH | 2.21.7 |
| jackson-core 2.21.4 | [CVE-2026-89425](https://osv.dev/vulnerability/GHSA-7hhh-6rmp-j9qf) | HIGH | 2.21.7 |
| jackson-core 2.21.4 | [CVE-2026-89407](https://osv.dev/vulnerability/GHSA-p6pp-m3f8-5c89) | HIGH | 2.21.7 |
| postgresql 42.7.11 | [CVE-2026-54291](https://osv.dev/vulnerability/GHSA-j92g-9f8w-j867) | HIGH | 42.7.12 |

## Melhorias de produto que precisam de aceite do cliente

Estes itens são limitações atuais; não devem ser apresentados como defeitos se estiverem fora do escopo contratado:

- Recuperação/redefinição segura de senha e troca obrigatória de senha inicial. Hoje o usuário precisa saber a senha atual; perder a senha do único administrador exige intervenção técnica.
- Correção/estorno de consumo vinculado à OS. Devolução manual altera estoque, mas não corrige o item da ordem; o mesmo material não pode ser lançado novamente na mesma OS.
- Edição, cancelamento ou reabertura de OS; edição de material, conta e perfil, conforme regras aprovadas.
- Quantidades fracionárias, se o cliente usa frações de metro, quilograma etc. Atualmente as quantidades são inteiras.
- Abertura de chamados pelo SOLICITANTE, se necessária. O perfil atual apenas consulta.
- Visibilidade por unidade, organização e solicitante. Hoje usuários autenticados consultam toda a base e assinaturas, conforme a regra documentada.
- Relatórios/exportação, anexos e notificações, se fizerem parte da entrega. Não estão implementados.

## Condições para liberar ao cliente

1. Corrigir A01, A02 e A03 e adicionar regressões permanentes. Tratar A04/A05 antes de exposição contínua e concluir triagem/atualização das dependências.
2. Corrigir códigos HTTP, abas, logout móvel e contraste. Acrescentar ao CI os testes exploratórios que hoje falham; os 17 testes atuais não os cobrem. Não há medição de cobertura de código configurada; não foi estimado um percentual.
3. Configurar e ensaiar PostgreSQL, migrações de uma base já existente, HTTPS/proxy, cookies Secure e ausência de dados demonstrativos no destino.
4. Automatizar backup e provar restauração em banco separado. Definir retenção de assinatura/auditoria, acesso aos backups e procedimento de recuperação. Este ensaio de restauração não foi executado.
5. Executar o JAR como serviço com reinício automático, logs úteis, verificação de saúde/prontidão, monitoramento e alertas. O Compose atual sobe somente PostgreSQL; não entrega o backend como serviço.
6. Medir carga com o volume e a simultaneidade reais esperados. Não foram executados teste prolongado, esgotamento de memória ou ensaio de múltiplas instâncias. Reiniciar a aplicação encerra sessões, e o rate limit também é por JVM.
7. Validar navegadores e celulares reais, assinatura por dedo/caneta, teclado virtual, rotação, expiração e perda de rede. A assinatura é imagem de atendimento, sem certificação digital implementada.
8. Preparar release identificável, instruções de instalação, atualização e rollback, contas e permissões aprovadas, treinamento e aceite dos fluxos do cliente. Há alterações locais anteriores à auditoria que precisam ser revisadas antes da release.

## Ambiente, reprodução e evidências

- JDK dos testes: Temurin 25.0.5 portátil. Node 24.22.3 e Playwright/axe foram preparados somente para a auditoria; a aplicação não passou a depender de Node.
- A execução direta de scripts `.ps1` foi bloqueada pela política deste Windows. A sintaxe foi validada; os testes PostgreSQL foram acionados por Maven/JDBC. O pacote PostgreSQL de testes não inclui `psql`, portanto o fluxo completo de `testar-postgres.ps1` não foi validado aqui. Documentar pré-requisitos e um procedimento de execução aprovado para a máquina do cliente, sem alterar políticas globais silenciosamente.
- `scripts/executar.ps1` verifica a existência de Java, mas não sua versão. Na máquina atual, um usuário que execute os comandos sem configurar o JDK continua usando Java 17. Melhorar o diagnóstico de pré-requisitos. A decisão de bootstrap H2 usa um caminho fixo; revisar o caso de URL H2 personalizada ou banco vazio criado por inicialização malsucedida.
- O Git exigiu `safe.directory` por diferença de proprietário do checkout. A auditoria usou uma opção por comando, sem alterar a configuração global.
- Logs de Maven: `target/logs/auditoria-verify-repeticao.log`, `target/logs/auditoria-verify-final.log` e `target/logs/auditoria-postgres.log`. O `clean verify` final passou em 28 segundos. As duas instâncias HTTP e o PostgreSQL temporário foram encerrados ao terminar.
- Resultados: `target/auditoria/http-results.json`, `context-results.json`, `browser-results.json`, `retention-results.json`, `dependency-results.json`, `powershell-syntax.json` e relatórios Surefire separados por banco.
- Capturas: `target/auditoria/desktop-os-concluida.png`, `mobile-conta.png` e `mobile-os-concluida.png`.
- Ensaios auxiliares: `.tools/auditoria/http-probes.mjs`, `browser-probes.mjs`, `dependency-scan.mjs`, `RetentionProbe.java` e `AuditDatabase.java`. São arquivos temporários de auditoria, não regressões incorporadas ao CI.
- `target/` e `.tools/` são ignorados pelo Git. Logs, capturas e ensaios são locais e podem desaparecer em limpezas futuras; este relatório registra os resultados e correções de forma persistente.

Para repetir o build com o JDK preparado nesta máquina:

```powershell
$env:JAVA_HOME = (Resolve-Path '.tools/jdk-25.0.5+8').Path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:MAVEN_USER_HOME = (Resolve-Path '.m2').Path
.\mvnw.cmd --batch-mode '-Dmaven.repo.local=.m2' clean verify
```

Os ensaios que usam `TEST_DATABASE_URL` apagam as tabelas do esquema de destino; executá-los somente em banco/esquema descartável. Para o ambiente do cliente, repetir com o procedimento isolado documentado em `docs/OPERACAO.md`.
