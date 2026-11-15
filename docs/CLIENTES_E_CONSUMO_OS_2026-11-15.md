# Clientes, OS e propriedade dos materiais

O terceiro problema era a ausência de um cliente cadastrado na OS: o solicitante era apenas texto,
e os estoques de clientes não tinham uma identidade compartilhada com o atendimento.

## Fluxo disponível

1. Entre com uma conta operacional da empresa e abra **Clientes → Novo cliente**. Informe código
   e nome. Administrador da empresa, supervisor, suporte técnico e estoquista podem cadastrar.
   As demais contas da empresa consultam. O master continua com a visão global de empresas e OS.
2. Em **Estoque → Novo estoque**, escolha Empresa ou Cliente. Para Cliente, selecione um cadastro
   da página Clientes. Pode haver vários estoques para o mesmo cliente. O vínculo é permanente.
3. Na nova OS, escolha um cliente cadastrado ou **Atendimento interno da empresa**. Solicitante
   continua sendo a pessoa que pediu o atendimento. A lista, a busca e os detalhes mostram o cliente.
4. Antes de consumir, classifique os materiais cujo proprietário você já identificou. O saldo e os
   registros anteriores permanecem iguais. Não existe classificação automática dos 169 materiais.

| Atendimento | Material da empresa | Material do mesmo cliente | De outro cliente | Sem classificação |
| --- | --- | --- | --- | --- |
| OS de cliente | Permite | Permite | Bloqueia | Bloqueia |
| Interno | Permite | Não se aplica | Bloqueia qualquer cliente | Preserva o uso antigo |
| Legado ainda não identificado | Permite | Não se aplica | Bloqueia qualquer cliente | Preserva o uso antigo |

A regra escolhida pelo usuário para OS de cliente é validada pelo servidor antes de qualquer baixa.
A interface também filtra os materiais. Tentativas diretas pela API retornam 409 e a transação
preserva saldo, itens, movimentações, auditoria e chaves de consumo.
Materiais de outra empresa continuam inacessíveis (404). Nenhuma permissão foi concedida pelo cliente.

## Compatibilidade e dados legados

OS anteriores recebem `tipoAtendimento=LEGADO`, com cliente nulo, sem inferir pelo solicitante.
Um administrador pode identificar uma OS legada que não esteja concluída nem tenha consumo.
O cliente de uma OS nova ou já identificada não pode ser trocado por esse endpoint. Históricos com
consumo precisam de um fluxo de correção próprio; nunca são reescritos para aceitar um vínculo.

Na API, omitir `clienteId` ou enviá-lo nulo numa nova OS significa atendimento **INTERNO**.
A interface exige uma escolha explícita. Para OS de cliente, envie um ID positivo da mesma empresa.
O cliente integra o resumo de idempotência. Repetir uma abertura idêntica retorna a mesma OS;
reutilizar a chave para outro cliente retorna 409. Os resumos antigos sem cliente são preservados.

## Migrations e contagens anteriores

O plano foi apresentado antes da criação dos arquivos. As mudanças são separadas por assunto:

- **V17 — clientes e atendimento:** cria `clientes`, acrescenta vínculos em estoques e ordens e
  distingue LEGADO/INTERNO/CLIENTE. Cada estoque CLIENTE antigo origina um cadastro com o mesmo
  código, nome e empresa, com novo ID da sequência compartilhada. Nomes semelhantes não são unidos.
- **V18 — integridade:** valida todas as contagens antes dos DDLs e acrescenta NOT NULL, chave
  primária, UNIQUE, CHECK e FKs compostas `(cliente_id, empresa_id)`. Um estoque CLIENTE exige cliente;
  EMPRESA exige vínculo nulo. Uma OS CLIENTE exige cliente; LEGADO/INTERNO exigem vínculo nulo.

A [consulta somente leitura para V16](sql/preflight-clientes-v16-postgresql.sql) projeta a conversão
sem gravar. A execução no PostgreSQL local, em 15/11/2026, encontrou:

| Verificação | Linhas incompatíveis |
| --- | ---: |
| `clientes_not_null` | 0 |
| `clientes_pkey` | 0 |
| `uq_clientes_id_empresa` | 0 |
| `uq_clientes_codigo_empresa` | 0 |
| `fk_clientes_empresa` | 0 |
| `ck_clientes_codigo` | 0 |
| `ck_clientes_nome` | 0 |
| `fk_estoques_cliente_empresa` | 0 |
| `ck_estoques_cliente` | 0 |
| `fk_ordens_cliente_empresa` | 0 |
| `ordens_tipo_atendimento_not_null` | 0 |
| `ck_ordens_cliente` | 0 |

Havia zero OS, zero estoques CLIENTE e 169 materiais sem classificação. Esses resultados representam
aquele instante. O próprio V18 repete as contagens no banco de destino e recusa incompatibilidades.
Nunca alterar ou excluir registros para forçar a aprovação; identificar a origem, propor uma correção
de negócio e obter a decisão do responsável. Não executar estas mudanças em produção nesta entrega.

## Caminho de volta

Os scripts manuais ficam em `src/main/resources/db/rollback/{postgresql,h2}`. Pare os escritores,
faça backup restaurável e reverta **V18 primeiro, V17 depois**, usando as variantes do seu banco.
PostgreSQL usa transação e lock com timeout; H2 exige manutenção exclusiva porque DDL tem autocommit.

V18 remove apenas a integridade nova. V17 recusa descartar qualquer atendimento identificado ou
cadastro/vínculo de cliente que não seja reconstruível a partir de exatamente um estoque CLIENTE
com o mesmo código, nome e empresa. Se recusar, preserve o schema e use correção à frente ou restaure
um backup compatível junto com a versão correspondente do aplicativo. Não limpar dados para passar.
Não remova o histórico do Flyway para simular uma reversão e não execute a aplicação nova após o down.
A sequência pode avançar na conversão e na reaplicação; lacunas de IDs não representam perda de dados.

## Validação

Testes cobrem abertura por cliente, isolamento entre empresas, propriedade no consumo, tentativas
diretas pela API, rollback de consumo bloqueado, repetição idempotente, preservação de histórico,
cadastro na interface, filtros e tela móvel. As migrations são ensaiadas nos dois bancos, incluindo
ida/volta/reaplicação com estoques CLIENTE preexistentes, dados incompatíveis e reversão recusada
para impedir perda de informação. Os testes usam bancos descartáveis; cadastros sintéticos não são
enviados ao banco operacional.

Resultado local: `mvn verify` aprovou 108 testes, a suíte PostgreSQL aprovou 77, e o navegador
aprovou 35, incluindo os cenários anteriores de master, permissões e recuperação após falhas.
O importador também passou no cluster descartável. Um backup local foi restaurado num segundo
cluster e atualizado para V18 pelo aplicativo: contagens e fingerprints das dez tabelas legadas
verificadas permaneceram iguais (materiais, estoques, ordens, itens, movimentos, usuários, empresas,
roles, notificações e idempotência). A tela móvel da OS foi inspecionada sem rolagem horizontal.

O CI identificou uma corrida na preparação da suíte: HTTP disponível não significa que o
ApplicationRunner já confirmou o administrador e os dados demo. A suíte operacional agora confirma
o acesso inicial antes de abrir o navegador e encerra essa sessão de sondagem. As tentativas limitadas
existem somente na preparação; os cenários de login mantêm suas verificações sem repetição automática.
