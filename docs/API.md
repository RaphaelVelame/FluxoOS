# API HTTP

Base: `/api`. JSON UTF-8. Erros retornam `{"mensagem":"Descrição"}`. A interface e a API usam a mesma origem. Todas as rotas, exceto login, exigem o cookie `fluxoos_sessao`.

1. `POST /auth/login` com `{"usuario":"admin","senha":"sua-senha"}` retorna o usuário e `csrf`, e emite o cookie HttpOnly.
2. Envie `X-CSRF-Token` nas operações POST, PUT e PATCH, inclusive logout.
3. Envie também `Idempotency-Key` com UUID na abertura de OS, no consumo e na movimentação manual.

| Método | Rota | Corpo / resultado |
| --- | --- | --- |
| GET | `/auth/me` | Conta atual |
| POST | `/auth/logout` | Revoga sessão |
| POST | `/auth/senha` | `atual`, `nova`; revoga todas as sessões |
| GET | `/usuarios` | Diretório sem hashes |
| POST | `/usuarios` | `login`, `nome`, `perfil`, `senha` |
| PATCH | `/usuarios/{id}/status` | `ativo` booleano |
| GET | `/ordens` | Listagem de OS |
| POST | `/ordens` | `solicitante`, `local`, `problema`, `prioridade` |
| GET | `/ordens/{id}` | Detalhes e itens |
| POST | `/ordens/{id}/atribuicao` | `tecnicoId` |
| POST | `/ordens/{id}/inicio` | Sem corpo |
| PUT | `/ordens/{id}/relato` | `relato` |
| POST | `/ordens/{id}/materiais` | `materialId`, `quantidade`, `observacao` |
| POST | `/ordens/{id}/conclusao` | `responsavel`, `assinatura` (data URL PNG) |
| GET | `/ordens/{id}/historico` | Auditoria da OS |
| GET | `/ordens/{id}/assinatura` | PNG autenticado |
| GET | `/materiais` | Saldos, alertas e `referenciaFabricante` (opcional, preenchida na importação) |
| GET | `/estoques` | Cadastros de estoques: `id`, `codigo`, `nome`, `tipo` (`EMPRESA`/`CLIENTE`) |
| POST | `/estoques` | `codigo`, `nome`, `tipo`; exige chefia ou suporte TI |
| POST | `/materiais` | `codigo`, `nome`, `unidade`, `minimo`, `estoqueId`; saldo inicial zero |
| PATCH | `/materiais/estoque` | `estoqueId`, `materialIds` (1–500); classifica materiais ainda sem proprietário |
| PATCH | `/materiais/{id}/status` | `ativo` booleano |
| POST | `/materiais/{id}/movimentos` | `tipo`, `variacao`, `observacao` |
| GET | `/movimentos` | Histórico do estoque |
| GET | `/seguranca/eventos` | Eventos, apenas chefia |

Prioridade: `BAIXA`, `MEDIA`, `ALTA`. Perfil: `CHEFE`, `SUPORTE_TI`, `TECNICO`, `SOLICITANTE`. Movimentos manuais: `ENTRADA`, `DEVOLUCAO`, `AJUSTE`. Ajuste recebe a **diferença**, por exemplo `-3`, não o saldo final. `SAIDA` é gerada apenas no consumo em OS.

Materiais, movimentos e itens de OS retornam `estoqueId`. Nos movimentos/itens, esse valor identifica
o estoque na época do registro; não é recalculado quando um material antigo é classificado.
`null` significa propriedade ainda não identificada. Consulte `/estoques` para o nome e o tipo.
O cadastro pela interface exige escolher um estoque. A API ainda aceita omitir `estoqueId` para
compatibilidade com integrações antigas; o material ficará sem classificação e poderá ser vinculado
depois. Uma referência informada precisa existir (404 se ausente, 400 se inválida).

`PATCH /materiais/estoque` exige chefia ou suporte TI e retorna `{"classificados":N}`. A transação
preserva saldo, movimentos e itens antigos, registra auditoria e cancela o lote inteiro se um material
não existir ou já pertencer a outro estoque. IDs repetidos são considerados uma vez; reenviar o mesmo
vínculo retorna zero alterações e não duplica auditoria. Trocar o proprietário de material já
classificado exige um fluxo de transferência, que não faz parte desta operação.

Status: 200 sucesso, 201 criação, 400 entrada inválida, 401 sem sessão/login recusado, 403 sem permissão/CSRF, 404 ausente, 409 conflito de estado/duplicidade, 413 requisição grande, 500 erro interno sem detalhes sensíveis. Todas as permissões estão na tabela do README.

Rotas inexistentes retornam 404; método HTTP incompatível retorna 405 com `Allow`, tipo de corpo
não suportado retorna 415, e identificador não numérico retorna 400. O limite do corpo da API é
300.000 bytes, inclusive em requisições sem `Content-Length` (envio em blocos); exceder retorna 413.

Desativar uma conta invalida todas as suas sessões anteriores. Reativar permite novo login e mantém os cookies antigos inválidos (401), inclusive quando um dispositivo não fez requisições durante a desativação. A invalidação acompanha o commit da alteração; rollback não encerra sessões de uma conta cuja alteração foi revertida.

`POST /ordens` exige `Idempotency-Key` de 16 a 80 letras, números ou hífens. Repetir a mesma chave e os mesmos dados com o mesmo autor retorna a mesma OS (201), sem repetir a abertura ou sua auditoria. O DTO representa o estado atual da ordem. Reutilizar a chave com outros dados retorna 409. Ausência ou formato inválido retorna 400. Uma nova abertura deve usar outra chave. Chaves de abertura são independentes das chaves de movimentação de estoque.

O login admite até 20 sessões ativas no total por instância. Credenciais válidas com capacidade esgotada retornam 429; as sessões existentes continuam válidas. Logout libera a vaga imediatamente, e sessões expiradas ou de contas invalidadas são descartadas. Cinco falhas por usuário em uma janela de 15 minutos bloqueiam novas autenticações por cinco minutos; o IP tem um controle adicional de 30 falhas na mesma janela e bloqueio de cinco minutos. Durante bloqueio, a API mantém 401 e a mensagem uniforme de autenticação recusada, sem revelar existência da conta. Tentativas recusadas durante o bloqueio não reiniciam seu prazo. Sessões já abertas não são encerradas por erros de senha em novos logins.
