# Revisão de schema — 12/11/2026

Status da coleta: relatório e plano apresentados antes da aprovação, sem alterar registros. Após a aprovação do usuário, V6–V12 foram criadas e testadas somente em bancos descartáveis; veja [implementação e reversão](MIGRACOES_INTEGRIDADE_2026-11-12.md). O banco operacional não recebeu essas versões. Nenhum registro foi alterado, removido ou preenchido para satisfazer regras. V1–V5, inclusive a V5 da importação anterior, foram preservadas.

## Escopo e evidências

Revisão de código front-end/back-end e catálogos reais do PostgreSQL local criado nesta conversa: localhost:5432/fluxoos, servidor PostgreSQL 18.6 on x86_64-windows, compiled by msvc-19.44.35229, 64-bit. Não houve acesso a ambiente de produção. As consultas usaram BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY, com transaction_read_only=on confirmado pelo servidor. Não foi executado DDL, nem foram iniciados testes que criassem tabelas nesta revisão.

Coleta do catálogo: 2026-11-12T20:31:13.641136-03:00. Contagens de regras: 2026-11-12T20:34:33.660398-03:00. Diagnóstico de modelagem: 2026-11-12T20:37:48.529917-03:00. Horários com fuso America/Bahia. As três coletas são snapshots independentes; os resultados não dispensam nova conferência no ambiente de testes antes de aplicar mudanças.

Há 8 tabelas e 63 colunas: sete tabelas da aplicação e uma tabela gerenciada pelo Flyway. Foram executadas 85 consultas de contagem de regras/garantias e consultas adicionais de modelagem. Todas as 31 constraints candidatas tiveram contagem zero de violações.

| Tabela | Linhas atuais |
| --- | --- |
| `auditoria` | 272 |
| `flyway_schema_history` | 5 |
| `idempotencia` | 0 |
| `itens_os` | 0 |
| `materiais` | 169 |
| `movimentos` | 169 |
| `ordens` | 0 |
| `usuarios` | 1 |

Limitação da evidência: ordens, itens_os e idempotencia estão vazias; os 169 movimentos atuais são ENTRADA. Contagem zero nessas regras significa ausência de linhas incompatíveis agora; não demonstra funcionamento de consumo, conclusão, concorrência ou idempotência. Esses fluxos precisam de fixtures e testes negativos em banco descartável.

Arquivos de evidência, sem senhas nem hashes de contas:

- [Catálogo completo](revisao-schema-2026-11-12/catalogo.json): colunas, defaults, constraints e índices.
- [SQL de pré-validação e resultados por consulta](revisao-schema-2026-11-12/preflight.sql): somente leitura; inclui as 85 consultas identificadas.
- [Contagens retornadas pelo banco](revisao-schema-2026-11-12/contagens.json).
- [Diagnósticos adicionais de modelagem](revisao-schema-2026-11-12/modelagem.json).
- [SQL dos diagnósticos de modelagem](revisao-schema-2026-11-12/modelagem.sql).

## 1. Inventário de tabelas, colunas e tipos

Tipos, nulabilidade e defaults abaixo foram lidos do catálogo real, não inferidos de anotações JPA. PK, FK, UNIQUE e CHECK existentes aparecem nas seções seguintes. Os IDs da aplicação recebem valores da sequência compartilhada sequencia_ids por meio do JPA; eles não possuem DEFAULT nextval no SQL atual.


### auditoria

| Coluna | Tipo PostgreSQL | Aceita NULL | DEFAULT |
| --- | --- | --- | --- |
| `id` | `bigint` | Não | — |
| `ordem_id` | `bigint` | Sim | — |
| `usuario_id` | `bigint` | Sim | — |
| `tipo` | `character varying(60)` | Não | — |
| `detalhes` | `character varying(1000)` | Não | — |
| `criado_em` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `alvo_id` | `bigint` | Sim | — |

### flyway_schema_history — interna do Flyway

| Coluna | Tipo PostgreSQL | Aceita NULL | DEFAULT |
| --- | --- | --- | --- |
| `installed_rank` | `integer` | Não | — |
| `version` | `character varying(50)` | Sim | — |
| `description` | `character varying(200)` | Não | — |
| `type` | `character varying(20)` | Não | — |
| `script` | `character varying(1000)` | Não | — |
| `checksum` | `integer` | Sim | — |
| `installed_by` | `character varying(100)` | Não | — |
| `installed_on` | `timestamp without time zone` | Não | `now()` |
| `execution_time` | `integer` | Não | — |
| `success` | `boolean` | Não | — |

### idempotencia

| Coluna | Tipo PostgreSQL | Aceita NULL | DEFAULT |
| --- | --- | --- | --- |
| `usuario_id` | `bigint` | Não | — |
| `chave` | `character varying(80)` | Não | — |
| `conteudo` | `character varying(64)` | Não | — |
| `movimento_id` | `bigint` | Não | — |

### itens_os

| Coluna | Tipo PostgreSQL | Aceita NULL | DEFAULT |
| --- | --- | --- | --- |
| `ordem_id` | `bigint` | Não | — |
| `material_id` | `bigint` | Não | — |
| `quantidade` | `integer` | Não | — |

### materiais

| Coluna | Tipo PostgreSQL | Aceita NULL | DEFAULT |
| --- | --- | --- | --- |
| `id` | `bigint` | Não | — |
| `codigo` | `character varying(60)` | Não | — |
| `nome` | `character varying(160)` | Não | — |
| `unidade` | `character varying(20)` | Não | — |
| `quantidade` | `integer` | Não | — |
| `quantidade_minima` | `integer` | Não | — |
| `ativo` | `boolean` | Não | — |
| `referencia_fabricante` | `character varying(160)` | Sim | — |

### movimentos

| Coluna | Tipo PostgreSQL | Aceita NULL | DEFAULT |
| --- | --- | --- | --- |
| `id` | `bigint` | Não | — |
| `material_id` | `bigint` | Não | — |
| `usuario_id` | `bigint` | Não | — |
| `ordem_id` | `bigint` | Sim | — |
| `tipo` | `character varying(30)` | Não | — |
| `variacao` | `integer` | Não | — |
| `saldo_apos` | `integer` | Não | — |
| `observacao` | `character varying(500)` | Não | — |
| `criado_em` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |

### ordens

| Coluna | Tipo PostgreSQL | Aceita NULL | DEFAULT |
| --- | --- | --- | --- |
| `id` | `bigint` | Não | — |
| `solicitante` | `character varying(160)` | Não | — |
| `local` | `character varying(240)` | Não | — |
| `problema` | `character varying(4000)` | Não | — |
| `prioridade` | `character varying(20)` | Não | — |
| `situacao` | `character varying(30)` | Não | — |
| `equipe_id` | `bigint` | Sim | — |
| `relato` | `character varying(8000)` | Sim | — |
| `responsavel` | `character varying(160)` | Sim | — |
| `assinatura` | `text` | Sim | — |
| `assinado_em` | `timestamp with time zone` | Sim | — |
| `criado_em` | `timestamp with time zone` | Não | `CURRENT_TIMESTAMP` |
| `autor_abertura_id` | `bigint` | Sim | — |
| `chave_abertura` | `character varying(80)` | Sim | — |
| `conteudo_abertura` | `character varying(64)` | Sim | — |

### usuarios

| Coluna | Tipo PostgreSQL | Aceita NULL | DEFAULT |
| --- | --- | --- | --- |
| `id` | `bigint` | Não | — |
| `nome_usuario` | `character varying(80)` | Não | — |
| `nome_completo` | `character varying(160)` | Não | — |
| `perfil` | `character varying(30)` | Não | — |
| `ativo` | `boolean` | Não | — |
| `hash_senha` | `character varying(300)` | Não | — |
| `versao_sessao` | `bigint` | Não | `0` |

## 2. Tipos inadequados e normalização

| Achado | Resultado observado | Encaminhamento |
| --- | --- | --- |
| Dinheiro em float/real/double | Nenhuma coluna monetária; nenhuma coluna com esses tipos no schema inspecionado. | Não há conversão monetária a fazer. |
| Timestamp sem fuso | Somente flyway_schema_history.installed_on, em 5 registros. As datas de negócio são timestamptz. | Não alterar tabela de controle de uma biblioteca. Sua interpretação depende da configuração do Flyway/JDBC; não presumir UTC nem converter os valores. |
| Data de negócio dentro de texto | 1 movimento contém a data original apenas em movimentos.observacao. | Propor posteriormente data_referencia DATE, distinta de criado_em. A fonte fornece somente o dia, sem horário. Preservar observacao; não extrair/backfill automaticamente nesta revisão. |
| Número em coluna textual | 167 códigos de material contêm somente dígitos. | São identificadores, não grandezas. Manter VARCHAR; códigos 164-L172 e SEM-COD-L199 também são válidos. Versões Flyway, chaves e digests igualmente não são números de cálculo. |
| Assinatura em TEXT | Data URL de uma imagem PNG; atualmente 0 assinaturas. | Falta limite/formato no banco. Acrescentar verificação parcial do envelope; imagem válida, dimensões e tinta permanecem no validador PNG. BYTEA seria uma evolução separada com contrato próprio. |
| Descricao repetida de material | 7 grupos por lower(btrim(nome)); 2 grupos também compartilham descrição/referência/unidade. | Não impor UNIQUE(nome) e não fundir saldos. Investigar se representam lotes/bobinas distintos antes de separar catálogo de produto e posição/lote de estoque. |
| Referencia de fabricante | 67 textos distintos atualmente. | O campo mistura marca/modelo/referência/notas. Um cadastro de fabricantes exigiria um mapeamento humano; igualdade de texto não prova identidade da entidade. |
| Saldo de material versus histórico | materiais.quantidade é derivável de movimentos.variacao; 0 divergências atuais. | Definir o razão de movimentos como fonte e quantidade como saldo materializado, ou usar uma visão de leitura. Não eliminar o campo sem adaptar serviço e concorrência. |
| Consumo repetido entre tabelas | itens_os.quantidade e a baixa negativa em movimentos representam o mesmo consumo; 0 divergências, mas ambas as consultas de consumo estão vazias. | Preferir vínculo 1:1 explícito do item com seu movimento em evolução separada, ou consistência transacional validada ao commit. |
| Snapshots de histórico | saldo_apos e detalhes de auditoria preservam fatos do momento da operação. | Não normalizar apagando snapshots nem reescrever fatos históricos com os dados atuais. |
| Pessoas e locais em texto | ordens.solicitante e responsavel são nomes do atendimento; local é descrição. | Não há evidência de que sejam usuários ou locais cadastrados. Não inventar FKs a usuarios pelo nome. |

Grupos de descrição repetida encontrados:

| Descrição normalizada | Registros | Códigos | Referências |
| --- | --- | --- | --- |
| cabo cat6. | 4 | 34, 35, 36, 37 | Condutti, SohoPlus |
| cabo de câmera | 2 | 38, 39 | Condutti |
| hdd 1tb | 3 | 131, 132, 133 | Hitachi, Samsung, Seagate |
| hdd 500gb | 3 | 137, 138, 139 | Dell, hgst, Samsung |
| mouse msi 50 | 2 | 157, 158 | Intelbras, LEHMOX |
| onu | 3 | 160, 161, 162 | FiberHome, MitraStar, SUMEC |
| roteador | 2 | 183, 184 | Core3, Huawei Claro |

Os dois grupos também iguais em referência/unidade são os cabos CAT6 SohoPlus (códigos 34, 35 e 36) e os cabos de câmera Condutti (38 e 39). Isso não autoriza somar seus saldos nem excluir registros. Nenhum UNIQUE de descrição/referência será criado nesta etapa.

A data em texto foi contada por SELECT count(*) FROM movimentos WHERE observacao ~ '^Entrada da planilha em [0-9]{4}-[0-9]{2}-[0-9]{2}: '; resultado: 1. Trata-se de um padrão conhecido do importador, não de uma busca universal de datas em texto livre. Nenhuma data foi convertida. Não se deve impor data_referencia NOT NULL ao legado sem uma decisão sobre entradas sem data de origem conhecida.

## 3. Referências e FOREIGN KEY

Não foi encontrada coluna relacional sem FK: as 12 referências estruturadas têm FOREIGN KEY declarada e as 12 contagens de órfãos retornaram zero. A varredura por sufixo _id foi cruzada com as entidades e os relacionamentos, não usada isoladamente.

| Origem | Destino | Órfãos atuais |
| --- | --- | --- |
| `auditoria.alvo_id` | `usuarios.id` | 0 |
| `auditoria.ordem_id` | `ordens.id` | 0 |
| `auditoria.usuario_id` | `usuarios.id` | 0 |
| `idempotencia.movimento_id` | `movimentos.id` | 0 |
| `idempotencia.usuario_id` | `usuarios.id` | 0 |
| `itens_os.material_id` | `materiais.id` | 0 |
| `itens_os.ordem_id` | `ordens.id` | 0 |
| `movimentos.material_id` | `materiais.id` | 0 |
| `movimentos.ordem_id` | `ordens.id` | 0 |
| `movimentos.usuario_id` | `usuarios.id` | 0 |
| `ordens.autor_abertura_id` | `usuarios.id` | 0 |
| `ordens.equipe_id` | `usuarios.id` | 0 |

A referência semântica que falta é composta: hoje idempotencia.usuario_id e idempotencia.movimento_id podem apontar para linhas válidas separadamente, mas o movimento pode pertencer a outro usuário. A FK composta candidata cobre essa combinação; sua pré-consulta retornou zero em uma tabela atualmente vazia.

Auditoria não é cadastro operacional: 45 eventos MATERIAL_IMPORTADO ainda mencionam IDs de materiais que foram excluídos a pedido do usuário; há 45 snapshots MATERIAL_REMOVIDO. São referências históricas dentro de detalhes, não colunas relacionais órfãs. Não adicionar uma FK que torne impossível preservar a auditoria de uma exclusão. Se o histórico for estruturado futuramente, separar identidade histórica de entidade operacional sem excluir esses eventos.

## 4. Validações já protegidas pelo banco

As oito CHECKs existentes, todas validadas, cobrem: perfil de usuário; prioridade e situação da OS; saldo e mínimo não negativos; quantidade de item positiva; variação de movimento diferente de zero; saldo após movimento não negativo. Todas as pré-consultas equivalentes retornaram zero.

Há PKs em todas as tabelas e unicidade de login, código de material, par OS/material, chave de idempotência por usuário e chave de abertura por autor. As consultas de duplicidade desses cinco conjuntos retornaram zero. O catálogo e o SQL de evidência detalham as garantias existentes; elas não serão recriadas com nomes novos.

Todos os campos incondicionalmente obrigatórios mapeados nas entidades já têm NOT NULL; as sete consultas de nulidade por tabela retornaram zero. Campos como técnico antes da atribuição, assinatura antes da conclusão, referência de fabricante e autores de eventos de login recusado continuam opcionais. Não há uma nova coluna justificando NOT NULL global nesta fase.

Os três metadados de abertura estão ausentes em parte do legado admitido pela V4 e pelo teste MigracaoSessaoTest. As consultas atuais de NULL retornam zero porque ordens está vazia; esse resultado não justifica invalidar bases legadas. O plano usa uma CHECK de conjunto completo ou totalmente ausente, em vez de impor três NOT NULL incondicionais.

VARCHAR(n) já limita os comprimentos das colunas correspondentes aos DTOs; assinatura TEXT ainda não tem o limite de 200.000 caracteres. Ressalva de equivalência: Java/JavaScript medem tamanho em unidades UTF-16, enquanto PostgreSQL char_length/VARCHAR contam caracteres. Caracteres fora do BMP podem diferir na contagem. Não afirmar equivalência byte a byte desses limites; testes Unicode devem definir a política antes de acrescentar checks extras de comprimento.

## 5. Constraints candidatas: pré-consultas executadas antes deste plano

Cada linha abaixo corresponde a uma consulta COUNT executada e identificada no preflight.sql. Os predicados completos estão nesse arquivo e no apêndice de SQL abaixo. Para UNIQUE, a consulta conta grupos duplicados; todos deram zero, portanto há também zero linhas conflitantes. Não existe tentativa de reparar dados automaticamente.

| Migration planejada | Constraint candidata | Regra | Violações atuais |
| --- | --- | --- | --- |
| V6 | `ck_usuarios_nome_completo_preenchido` | nome_completo: texto preenchido e já aparado | 0 |
| V6 | `ck_materiais_codigo_preenchido` | codigo: texto preenchido e já aparado | 0 |
| V6 | `ck_materiais_nome_preenchido` | nome: texto preenchido e já aparado | 0 |
| V6 | `ck_materiais_unidade_preenchido` | unidade: texto preenchido e já aparado | 0 |
| V6 | `ck_ordens_solicitante_preenchido` | solicitante: texto preenchido e já aparado | 0 |
| V6 | `ck_ordens_local_preenchido` | local: texto preenchido e já aparado | 0 |
| V6 | `ck_ordens_problema_preenchido` | problema: texto preenchido e já aparado | 0 |
| V6 | `ck_ordens_relato_preenchido` | relato: texto preenchido e já aparado quando presente | 0 |
| V6 | `ck_ordens_responsavel_preenchido` | responsavel: texto preenchido e já aparado quando presente | 0 |
| V6 | `ck_movimentos_observacao_preenchido` | observacao: texto preenchido e já aparado | 0 |
| V6 | `ck_auditoria_tipo_preenchido` | tipo: texto preenchido e já aparado | 0 |
| V6 | `ck_auditoria_detalhes_preenchido` | detalhes: texto preenchido e já aparado | 0 |
| V7 | `ck_usuarios_login_formato` | login canônico: 3–80 caracteres ASCII minúsculos | 0 |
| V7 | `ck_usuarios_hash_formato` | envelope PBKDF2 compatível com o verificador atual | 0 |
| V7 | `ck_usuarios_versao_sessao_nao_negativa` | versão de sessão não negativa | 0 |
| V8 | `ck_movimentos_tipo` | tipo pertence ao enum | 0 |
| V8 | `ck_movimentos_sinal` | entrada/devolução positivas; saída negativa; ajuste não zero | 0 |
| V8 | `ck_movimentos_ordem_por_tipo` | saída exige OS; entrada, devolução e ajuste manuais não possuem OS | 0 |
| V9 | `ck_ordens_tecnico_apos_inicio` | OS iniciada/concluída possui técnico | 0 |
| V9 | `ck_ordens_conclusao_completa` | conclusão exige relato, responsável, assinatura e instante | 0 |
| V9 | `ck_ordens_conclusao_exclusiva` | campos de assinatura só são preenchidos ao concluir | 0 |
| V9 | `ck_ordens_relato_apos_inicio` | relato só existe após iniciar atendimento | 0 |
| V9 | `ck_ordens_assinatura_envelope` | assinatura: prefixo PNG/base64 e limite de 200.000 caracteres; validação parcial | 0 |
| V10 | `ck_ordens_abertura_conjunto` | metadados de abertura todos ausentes (legado) ou todos presentes | 0 |
| V10 | `ck_ordens_chave_abertura` | chave de abertura de 16–80 caracteres alfanuméricos/hífen | 0 |
| V10 | `ck_ordens_conteudo_abertura` | digest SHA-256 hexadecimal minúsculo com 64 caracteres | 0 |
| V10 | `ck_idempotencia_chave` | chave de movimento de 16–80 caracteres alfanuméricos/hífen | 0 |
| V10 | `ck_idempotencia_conteudo` | digest SHA-256 hexadecimal minúsculo com 64 caracteres | 0 |
| V11 | `uq_movimentos_id_usuario` | UNIQUE (id,usuario_id): chave de suporte para FK composta; grupos duplicados | 0 |
| V11 | `fk_idempotencia_movimento_autor` | FK composta garante que o dono da chave é o autor do movimento | 0 |
| V12 | `uq_movimentos_consumo_os_material` | um consumo por material/OS; grupos duplicados | 0 |

As primeiras 12 checks de texto impedem conteúdo vazio/somente espaços e texto com controles ASCII nas extremidades que o Java apararia antes de persistir; relato e responsável aceitam NULL enquanto opcionais. O predicado diferencia os conjuntos de whitespace de String.isBlank e String.trim, em vez de presumir que btrim simples ou uma classe SQL genérica reproduza Unicode do Java.

A regra de assinatura verifica somente envelope, alfabeto base64 e tamanho textual. Não demonstra decodificação completa, cabeçalho PNG válido, dimensões 50x30 a 2048x2048 nem pelo menos 20 pixels de tinta. Essas validações estão em Assinatura.validar e continuarão no back-end; nenhuma função de imagem ou extensão será instalada para simular equivalência.

A CHECK de hash limita o envelope ao formato aceito hoje pelo verificador (PBKDF2-SHA256, iterações 600.000–2.000.000, salt e digest base64url). Uma futura troca de algoritmo exigirá revisar essa regra. O comprimento da senha original não pode ser inferido do hash: não cabe CHECK de 15 caracteres em hash_senha. Há uma exceção de senha do admin solicitada anteriormente pelo titular; nenhuma senha será redefinida para esta revisão.

Compatibilidade de teste identificada antes das migrations: MigracaoSessaoTest usa o literal hash-preservado como fixture, fora do envelope PBKDF2. A nova regra deverá ter teste explícito de recusa desse legado inválido sem alterar seu conteúdo, e outro teste de preservação literal de um hash válido. Não inserir exceção para esse texto no banco nem trocar silenciosamente hashes reais.

## 6. Plano de migrations e caminhos de volta — aprovado e implementado

| Versão e assunto | Mudança planejada | Caminho de volta |
| --- | --- | --- |
| V6__textos_obrigatorios | 12 CHECKs de preenchimento/normalização; preservar NULL nos campos opcionais. | DROP somente das 12 constraints criadas por V6; não UPDATE/DELETE. |
| V7__integridade_usuarios | CHECK do login canônico, envelope de hash e versão de sessão >= 0. | DROP das 3 checks de V7; nenhum reset de senha. |
| V8__regras_movimentos | CHECK do tipo, do sinal por tipo e da presença de OS conforme o tipo. | DROP das 3 checks de V8; manter checks/FKs originais. |
| V9__consistencia_ordens | 5 CHECKs: técnico após início, conclusão completa, assinatura exclusiva da conclusão, relato após início e envelope da imagem. | DROP das 5 checks de V9; manter estado, assinatura e relato intactos. |
| V10__formato_idempotencia | 5 CHECKs de chaves/digests e conjunto de metadados de abertura, com compatibilidade de legado. | DROP das 5 checks de V10; manter a UNIQUE de V4. |
| V11__autor_movimento_idempotencia | UNIQUE movimentos(id,usuario_id) de suporte e FK idempotencia(movimento_id,usuario_id) para esse par. | DROP primeiro da FK composta, depois da UNIQUE de suporte; manter FKs simples existentes. |
| V12__consumo_unico_por_os | UNIQUE movimentos(ordem_id,material_id), com NULLs distintos; V8 garante que apenas SAIDA carrega OS. | DROP somente dessa UNIQUE. Entradas sem OS continuam podendo repetir material. |

Organização proposta: preservar V1–V5 em src/main/resources/db/migration e manter as novas versões em diretórios irmãos selecionados pelo perfil do banco, por exemplo db/postgresql e db/h2. Não colocar variantes dos dois bancos em subpastas do diretório comum que o Flyway percorre recursivamente. As expressões PostgreSQL deste relatório não serão copiadas cegamente para H2; equivalentes e diferenças de suporte precisam de testes dedicados e aprovação explícita se houver perda de cobertura.

Cada versão tem seu arquivo de ida e um arquivo de reversão correspondente fora das locations de migrations (por exemplo db/rollback/postgresql/V6__textos_obrigatorios.down.sql), além da variante H2. As configurações programáticas de Flyway dos testes também selecionam explicitamente a variante do banco; o placeholder de vendor do Spring Boot não foi presumido como recurso do Flyway programático.

Com Flyway versionado, um arquivo down não é executado automaticamente por ter esse nome. Para reverter uma versão já aplicada sem mecanismo de undo habilitado, preferir uma migration compensatória com versão posterior contendo os DROP planejados; não apagar linhas de flyway_schema_history nem reescrever V1–V5. Os scripts de reversão serão verificados em ordem inversa, com a FK composta removida antes de seu índice/UNIQUE de suporte.

Aplicação prevista somente depois da aprovação do plano: criar os arquivos, repetir as pré-consultas e ensaiar ida, rejeição de INSERT/UPDATE inválido e volta em bancos descartáveis PostgreSQL/H2. Nenhuma aplicação no banco operacional ou em produção está autorizada por este plano. Caso qualquer pré-consulta passe a apontar violações, interromper a migration correspondente e apresentar contagem/IDs e opções de tratamento sem executar saneamento.

NOT VALID pode reduzir a varredura inicial de CHECKs/FKs no PostgreSQL, mas ainda restringe INSERT/UPDATE novos e não prova que o legado está íntegro. Não será usado para mascarar violações nem apresentado como migração concluída sem validação. DDL exige avaliar locks mesmo com tabelas pequenas. [Referência oficial PostgreSQL](https://www.postgresql.org/docs/18/sql-altertable.html).

## 7. Regras que exigem outro mecanismo ou decisão de negócio

| Regra | Pré-leitura atual | Tratamento a planejar |
| --- | --- | --- |
| materiais cujo saldo difere da soma de movimentos | 0 | Não cabe em CHECK de uma única linha; avaliar vínculo declarativo adicional ou constraint trigger diferida, com locking e testes de concorrência. |
| movimentos cujo saldo_apos difere do acumulado por id (ordem técnica, não data do evento) | 0 | Não cabe em CHECK de uma única linha; avaliar vínculo declarativo adicional ou constraint trigger diferida, com locking e testes de concorrência. |
| saídas sem item de OS de mesma quantidade | 0 | Não cabe em CHECK de uma única linha; avaliar vínculo declarativo adicional ou constraint trigger diferida, com locking e testes de concorrência. |
| itens de OS sem saída correspondente | 0 | Não cabe em CHECK de uma única linha; avaliar vínculo declarativo adicional ou constraint trigger diferida, com locking e testes de concorrência. |
| OS atribuídas a usuário de perfil diferente de TECNICO | 0 | Validação no evento de atribuição; não tornar atividade atual uma obrigação de todo histórico. |

Saldo atual versus razão, saldo_apos versus sequência e vínculo item/baixa precisam de uma única política de escrita. Um trigger que atualizasse o saldo hoje em paralelo ao EstoqueService duplicaria a movimentação. Primeiro definir se o banco calcula o saldo ou apenas valida, e testar JPA/flush/commit e a importação, que grava o saldo e depois seus movimentos na mesma transação. A ordenação por ID na consulta de saldo_apos é um diagnóstico técnico compatível com a carga atual, não uma garantia universal de cronologia dos eventos.

Não usar CHECK com subconsulta ou função que lê outras tabelas para simular essa integridade. [A documentação do PostgreSQL delimita CHECK à linha verificada](https://www.postgresql.org/docs/18/ddl-constraints.html).

Transições ABERTA → EM_ANDAMENTO → CONCLUIDA, proibição de editar uma OS concluída, material ativo no momento do uso e autor autorizado exigem considerar OLD/NEW, identidade do ator ou estado no momento do evento. Não há histórico completo suficiente para contar violações passadas dessas transições com uma consulta de estado atual; não relatar zero como se isso tivesse sido comprovado. Mantê-las na aplicação até aprovar um desenho específico de triggers/procedures/permissões. Desativar um técnico ou material depois de um evento válido não torna o evento passado inválido.

Permissões, senha atual, confirmação de senha, CSRF, limite de sessões e tentativas de login são validações de requisição/segurança; sessões e limitadores atuais ficam em memória. Não há uma NOT NULL/UNIQUE/CHECK/FK equivalente que substitua esse fluxo. O banco compartilha uma conta de conexão da aplicação e não identifica automaticamente o usuário humano.

Não impor quantidade > 0 ao cadastro só porque os materiais zerados foram removidos anteriormente: o código cadastra material com saldo zero, e um material pode chegar a zero após consumo. A regra correta continua quantidade >= 0. Não restringir unidade aos dois valores observados na planilha: a interface admite outras unidades, como kg.

Decisões adiadas: produto versus lote/bobina; cadastro de fabricantes; data_referencia de movimentos antigos; armazenamento binário de assinatura; validações temporais entre linhas. Não há migrations desses assuntos nesta etapa.

## 8. Validação prevista após aprovação

- Garantir que dados válidos passam e dados inválidos são rejeitados diretamente por SQL, sem depender do front/back.
- Cobrir NULL, branco, controles, Unicode fora do BMP, limites de tamanho, enums/sinais, abertura legada completa/ausente/parcial e conclusão incompleta.
- Criar movimentos de autores diferentes para demonstrar a FK composta; testar repetição de chave e de consumo.
- Testar os fluxos reais de cadastro com zero, entrada, ajuste negativo, consumo, conclusão e importação, inclusive concorrência e ordens de flush.
- Comparar linhas e valores antes/depois da ida e volta; somente metadados de constraints podem mudar. Preservar expressamente hashes, textos, snapshots e assinaturas.
- Executar testes em bases descartáveis, sem iniciar a aplicação contra dados operacionais com migrations pendentes. Não executar nada em produção.

## 9. Fontes de código examinadas

Entidades em src/main/java/br/com/fluxoos/entity, DTOs em dto/Contratos.java, services EstoqueService, OrdemServicoService, UsuarioService, TrocaSenhaService e Assinatura, normalização em seguranca/Identificadores.java, hash em seguranca/SenhaPbkdf2.java, repositórios e mapeamentos JPA, migrations V1–V5, formulários em static/js/views, helper campo de components.js, captura signature.js, autenticação app.js, importador scripts/importar-estoque.ps1 e teste de compatibilidade MigracaoSessaoTest.

## 10. Apêndice: SQL de pré-validação de cada constraint candidata

Os blocos seguintes já foram executados dentro da transação READ ONLY; não são migrations e não criam objetos.


### ck_usuarios_nome_completo_preenchido

nome_completo: texto preenchido e já aparado. Origem: entity/Validacoes.java; dto/Contratos.java; formulários required.

```sql
SELECT count(*) FROM public.usuarios WHERE ((length(regexp_replace(nome_completo, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (nome_completo = regexp_replace(nome_completo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_materiais_codigo_preenchido

codigo: texto preenchido e já aparado. Origem: entity/Validacoes.java; dto/Contratos.java; formulários required.

```sql
SELECT count(*) FROM public.materiais WHERE ((length(regexp_replace(codigo, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (codigo = regexp_replace(codigo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_materiais_nome_preenchido

nome: texto preenchido e já aparado. Origem: entity/Validacoes.java; dto/Contratos.java; formulários required.

```sql
SELECT count(*) FROM public.materiais WHERE ((length(regexp_replace(nome, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (nome = regexp_replace(nome, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_materiais_unidade_preenchido

unidade: texto preenchido e já aparado. Origem: entity/Validacoes.java; dto/Contratos.java; formulários required.

```sql
SELECT count(*) FROM public.materiais WHERE ((length(regexp_replace(unidade, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (unidade = regexp_replace(unidade, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_ordens_solicitante_preenchido

solicitante: texto preenchido e já aparado. Origem: entity/Validacoes.java; dto/Contratos.java; formulários required.

```sql
SELECT count(*) FROM public.ordens WHERE ((length(regexp_replace(solicitante, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (solicitante = regexp_replace(solicitante, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_ordens_local_preenchido

local: texto preenchido e já aparado. Origem: entity/Validacoes.java; dto/Contratos.java; formulários required.

```sql
SELECT count(*) FROM public.ordens WHERE ((length(regexp_replace(local, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (local = regexp_replace(local, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_ordens_problema_preenchido

problema: texto preenchido e já aparado. Origem: entity/Validacoes.java; dto/Contratos.java; formulários required.

```sql
SELECT count(*) FROM public.ordens WHERE ((length(regexp_replace(problema, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (problema = regexp_replace(problema, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_ordens_relato_preenchido

relato: texto preenchido e já aparado quando presente. Origem: entity/Validacoes.java; dto/Contratos.java; formulários required.

```sql
SELECT count(*) FROM public.ordens WHERE (relato IS NULL OR ((length(regexp_replace(relato, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (relato = regexp_replace(relato, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')))) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_ordens_responsavel_preenchido

responsavel: texto preenchido e já aparado quando presente. Origem: entity/Validacoes.java; dto/Contratos.java; formulários required.

```sql
SELECT count(*) FROM public.ordens WHERE (responsavel IS NULL OR ((length(regexp_replace(responsavel, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (responsavel = regexp_replace(responsavel, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g')))) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_movimentos_observacao_preenchido

observacao: texto preenchido e já aparado. Origem: entity/Validacoes.java; dto/Contratos.java; formulários required.

```sql
SELECT count(*) FROM public.movimentos WHERE ((length(regexp_replace(observacao, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (observacao = regexp_replace(observacao, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_auditoria_tipo_preenchido

tipo: texto preenchido e já aparado. Origem: entity/Validacoes.java; dto/Contratos.java; formulários required.

```sql
SELECT count(*) FROM public.auditoria WHERE ((length(regexp_replace(tipo, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (tipo = regexp_replace(tipo, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_auditoria_detalhes_preenchido

detalhes: texto preenchido e já aparado. Origem: entity/Validacoes.java; dto/Contratos.java; formulários required.

```sql
SELECT count(*) FROM public.auditoria WHERE ((length(regexp_replace(detalhes, U&'[\0009-\000D\001C-\0020\1680\2000-\2006\2008-\200A\2028\2029\205F\3000]', '', 'g')) > 0) AND (detalhes = regexp_replace(detalhes, U&'^[\0001-\0020]+|[\0001-\0020]+$', '', 'g'))) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_usuarios_login_formato

login canônico: 3–80 caracteres ASCII minúsculos. Origem: service/UsuarioService.java; seguranca/Identificadores.java; views/team.js.

```sql
SELECT count(*) FROM public.usuarios WHERE (nome_usuario ~ '^[a-z0-9._-]{3,80}$') IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_usuarios_hash_formato

envelope PBKDF2 compatível com o verificador atual. Origem: seguranca/SenhaPbkdf2.java.

```sql
SELECT count(*) FROM public.usuarios WHERE (CASE WHEN hash_senha ~ '^pbkdf2_sha256[$][0-9]{6,7}[$][A-Za-z0-9_-]{22}[$][A-Za-z0-9_-]{43}$' THEN split_part(hash_senha,'$',2)::integer BETWEEN 600000 AND 2000000 ELSE false END) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_usuarios_versao_sessao_nao_negativa

versão de sessão não negativa. Origem: V3; entity/Usuario.java; repository/UsuarioRepository.java (invariante inferida).

```sql
SELECT count(*) FROM public.usuarios WHERE (versao_sessao >= 0) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_movimentos_tipo

tipo pertence ao enum. Origem: enums/TipoMovimento.java; dto/Contratos.java.

```sql
SELECT count(*) FROM public.movimentos WHERE (tipo IN ('ENTRADA','SAIDA','DEVOLUCAO','AJUSTE')) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_movimentos_sinal

entrada/devolução positivas; saída negativa; ajuste não zero. Origem: service/EstoqueService.java.

```sql
SELECT count(*) FROM public.movimentos WHERE ((tipo IN ('ENTRADA','DEVOLUCAO') AND variacao > 0) OR (tipo='SAIDA' AND variacao < 0) OR (tipo='AJUSTE' AND variacao <> 0)) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_movimentos_ordem_por_tipo

saída exige OS; entrada, devolução e ajuste manuais não possuem OS. Origem: service/EstoqueService.java: movimentar versus consumir.

```sql
SELECT count(*) FROM public.movimentos WHERE ((tipo='SAIDA' AND ordem_id IS NOT NULL) OR (tipo IN ('ENTRADA','DEVOLUCAO','AJUSTE') AND ordem_id IS NULL)) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_ordens_tecnico_apos_inicio

OS iniciada/concluída possui técnico. Origem: service/OrdemServicoService.java: iniciar.

```sql
SELECT count(*) FROM public.ordens WHERE (situacao='ABERTA' OR equipe_id IS NOT NULL) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_ordens_conclusao_completa

conclusão exige relato, responsável, assinatura e instante. Origem: entity/OrdemServico.java: concluirComAssinatura.

```sql
SELECT count(*) FROM public.ordens WHERE (situacao <> 'CONCLUIDA' OR (relato IS NOT NULL AND responsavel IS NOT NULL AND assinatura IS NOT NULL AND assinado_em IS NOT NULL)) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_ordens_conclusao_exclusiva

campos de assinatura só são preenchidos ao concluir. Origem: entity/OrdemServico.java: concluirComAssinatura.

```sql
SELECT count(*) FROM public.ordens WHERE (situacao='CONCLUIDA' OR (responsavel IS NULL AND assinatura IS NULL AND assinado_em IS NULL)) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_ordens_relato_apos_inicio

relato só existe após iniciar atendimento. Origem: entity/OrdemServico.java: registrarRelato.

```sql
SELECT count(*) FROM public.ordens WHERE (relato IS NULL OR situacao IN ('EM_ANDAMENTO','CONCLUIDA')) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_ordens_assinatura_envelope

assinatura: prefixo PNG/base64 e limite de 200.000 caracteres; validação parcial. Origem: service/Assinatura.java; dto/Contratos.java.

```sql
SELECT count(*) FROM public.ordens WHERE (assinatura IS NULL OR (char_length(assinatura) <= 200000 AND assinatura ~ '^data:image/png;base64,[A-Za-z0-9+/]+={0,2}$')) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_ordens_abertura_conjunto

metadados de abertura todos ausentes (legado) ou todos presentes. Origem: V4; service/OrdemServicoService.java; test/MigracaoSessaoTest.java.

```sql
SELECT count(*) FROM public.ordens WHERE (num_nonnulls(autor_abertura_id,chave_abertura,conteudo_abertura) IN (0,3)) IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_ordens_chave_abertura

chave de abertura de 16–80 caracteres alfanuméricos/hífen. Origem: service/OrdemServicoService.java.

```sql
SELECT count(*) FROM public.ordens WHERE (chave_abertura IS NULL OR chave_abertura ~ '^[a-zA-Z0-9-]{16,80}$') IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_ordens_conteudo_abertura

digest SHA-256 hexadecimal minúsculo com 64 caracteres. Origem: service/OrdemServicoService.java.

```sql
SELECT count(*) FROM public.ordens WHERE (conteudo_abertura IS NULL OR conteudo_abertura ~ '^[0-9a-f]{64}$') IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_idempotencia_chave

chave de movimento de 16–80 caracteres alfanuméricos/hífen. Origem: service/EstoqueService.java.

```sql
SELECT count(*) FROM public.idempotencia WHERE (chave ~ '^[a-zA-Z0-9-]{16,80}$') IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### ck_idempotencia_conteudo

digest SHA-256 hexadecimal minúsculo com 64 caracteres. Origem: service/EstoqueService.java.

```sql
SELECT count(*) FROM public.idempotencia WHERE (conteudo ~ '^[0-9a-f]{64}$') IS NOT TRUE;
```

Resultado: **0** linhas incompatíveis.

### uq_movimentos_id_usuario

UNIQUE (id,usuario_id): chave de suporte para FK composta; grupos duplicados. Origem: Pré-requisito PostgreSQL para FK composta; não substitui PK(id).

```sql
SELECT count(*) FROM (SELECT id,usuario_id FROM public.movimentos GROUP BY id,usuario_id HAVING count(*)>1) d;
```

Resultado: **0** grupos duplicados.

### fk_idempotencia_movimento_autor

FK composta garante que o dono da chave é o autor do movimento. Origem: service/EstoqueService.java: registrarRequisicao.

```sql
SELECT count(*) FROM public.idempotencia i WHERE NOT EXISTS(SELECT 1 FROM public.movimentos m WHERE m.id=i.movimento_id AND m.usuario_id=i.usuario_id);
```

Resultado: **0** linhas incompatíveis.

### uq_movimentos_consumo_os_material

um consumo por material/OS; grupos duplicados. Origem: entity/OrdemServico.java: validarInclusaoItem; itens_os PK.

```sql
SELECT count(*) FROM (SELECT ordem_id,material_id FROM public.movimentos WHERE ordem_id IS NOT NULL GROUP BY ordem_id,material_id HAVING count(*)>1) d;
```

Resultado: **0** grupos duplicados.
