# API HTTP

Base: `/api`. JSON UTF-8. Erros retornam `{"mensagem":"Descrição"}`. A interface e a API usam a mesma origem. Todas as rotas, exceto login, exigem o cookie `fluxoos_sessao`.

1. `POST /auth/login` com `{"usuario":"admin","senha":"sua-senha"}` retorna o usuário e `csrf`, e emite o cookie HttpOnly.
2. Envie `X-CSRF-Token` nas operações POST, PUT e PATCH, inclusive logout.
3. Envie também `Idempotency-Key` com UUID em consumo e movimentação manual.

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
| GET | `/materiais` | Saldos e alertas |
| POST | `/materiais` | `codigo`, `nome`, `unidade`, `minimo`; saldo inicial zero |
| PATCH | `/materiais/{id}/status` | `ativo` booleano |
| POST | `/materiais/{id}/movimentos` | `tipo`, `variacao`, `observacao` |
| GET | `/movimentos` | Histórico do estoque |
| GET | `/seguranca/eventos` | Eventos, apenas chefia |

Prioridade: `BAIXA`, `MEDIA`, `ALTA`. Perfil: `CHEFE`, `SUPORTE_TI`, `TECNICO`, `SOLICITANTE`. Movimentos manuais: `ENTRADA`, `DEVOLUCAO`, `AJUSTE`. Ajuste recebe a **diferença**, por exemplo `-3`, não o saldo final. `SAIDA` é gerada apenas no consumo em OS.

Status: 200 sucesso, 201 criação, 400 entrada inválida, 401 sem sessão/login recusado, 403 sem permissão/CSRF, 404 ausente, 409 conflito de estado/duplicidade, 413 requisição grande, 500 erro interno sem detalhes sensíveis. Todas as permissões estão na tabela do README.
