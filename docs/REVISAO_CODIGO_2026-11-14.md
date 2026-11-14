# Revisão de código — 14/11/2026

Revisão da autenticação, RBAC, consultas por empresa, fluxo de OS/estoque, eventos,
configuração e interface. Os testes usam H2 em memória e PostgreSQL descartável. Nenhuma
conta, senha ou dado operacional foi alterado nesta revisão; não houve nova migration nem
reinicialização do aplicativo operacional.

## Problemas corrigidos

| Problema | Evidência | Correção |
| --- | --- | --- |
| Master autenticava, mas a interface ficava presa na atualização | A tela carregava `/materiais`, `/movimentos` e `/estoques`, que exigem empresa operacional e retornam 403 para master global; regressão reproduzida no JAR anterior | Carregamento próprio de empresas, usuários e OS; visão global de consulta; estoque ocultado na navegação master e acesso direto tratado sem consulta indevida |
| Um 401 antigo encerrava uma sessão nova | Retendo uma consulta até depois de logout e novo login, a resposta anterior apagava o CSRF novo e abria a tela de login; teste reproduziu o erro | Cada chamada captura a geração da sessão. Respostas de outra geração são descartadas antes de alterar autenticação ou entregar dados |
| Detalhes pendentes de uma conta podiam renderizar depois da troca de empresa | O detalhe de OS verificava somente o hash da rota, sem conferir o usuário que iniciou a consulta | Conferência da identidade no detalhe e no tratamento de erro da rota; callbacks de modais e eventos de segurança também ficam vinculados à sessão que os originou |
| Opção de bootstrap master do `.env` era ignorada pelo inicializador | `BOOTSTRAP_ADMIN_MASTER` existia na configuração/documentação, mas faltava na lista de variáveis carregadas por `scripts/executar.ps1` | Inclusão na lista com a mesma precedência e restauração das demais variáveis; continua sem promover contas existentes |
| Avatar começava com “?” e permanecia assim | O shell consultava as iniciais no diretório ainda não carregado | Usa o nome do próprio usuário autenticado como alternativa, antes do diretório |

O backend mantém as regras anteriores: master global não recebe tenant implícito nem
permissão operacional de estoque. A visão global mostra empresas, usuários e OS, sem
apresentar indicadores fictícios de estoque vazio/em dia. O provisionamento permanece na API.

## Regressões e resultados

Dois testes novos falharam contra o JAR anterior: entrada master e 401 de sessão anterior.
Passaram após a correção. Um terceiro ensaio retém uma resposta autorizada ao master,
troca para uma conta de outra empresa, solicita o mesmo ID de OS e libera a resposta antiga
por último. A conta nova permanece na página de recurso indisponível, sem renderizar os
dados da empresa anterior.

A preparação dos testes aguarda o carregamento efetivo da página. No servidor de teste
master, a sondagem inicial admite até três tentativas de autenticação para esperar o
ApplicationRunner confirmar a primeira conta; fica abaixo do limite de bloqueio. A suíte
operacional força bootstrap master desligado, independentemente do ambiente do terminal.

| Verificação | Resultado |
| --- | --- |
| Maven verify, Java 25/H2 | 97 testes; zero falhas, erros ou ignorados |
| PostgreSQL 18 descartável | 66 testes; zero falhas, erros ou ignorados |
| Interface em Edge/Playwright, dois servidores H2 próprios | 28 testes; zero falhas ou ignorados |
| Importação sintética | Preservação, reimportação, conflito e rollback aprovados |
| JavaScript | 11 arquivos com sintaxe verificada |
| Inicializador PowerShell | Análise sintática aprovada |
| Whitespace Git | `git diff --check` aprovado |

A visão master foi conferida em desktop e celular de 390 px, sem ultrapassar a largura da
tela. As capturas locais ficam em `target/e2e/master-desktop.png` e `master-mobile.png`.
Os testes de navegador também verificam recarga da página, atualização, consulta global,
ausência de requisições de estoque e iniciais do avatar.

O código Java não precisou de alteração. O empacotamento final, incluindo as correções
estáticas, fica em `target/revisao-14-final/fluxoos-1.0.0-SNAPSHOT.jar`. Os logs dos ensaios
ficam em `target/revisao-14-*.log`, ignorados pelo Git. O GitHub Actions executa ambas as
suítes de navegador pelo comando `npm test`.

```powershell
.\mvnw.cmd -Dfluxoos.build.directory=target/revisao-14 verify
.\scripts\testar-postgres.ps1 -IncluirImportacao
$env:FLUXOOS_TEST_JAR = (Resolve-Path target/revisao-14/fluxoos-1.0.0-SNAPSHOT.jar).Path
$env:BROWSER_CHANNEL = 'msedge'
npm test --prefix tests/e2e
```

Esta entrega publica código e testes. A atualização do aplicativo em execução é uma etapa
de implantação separada. Permanecem as condições operacionais e a avaliação de dependências
já registradas nos documentos de [SaaS/RBAC](SAAS_RBAC_2026-11-13.md) e
[revisão anterior](REVISAO_FINAL_2026-11-12.md).
