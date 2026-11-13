# Migrations de integridade — 12/11/2026

Implementação do [plano aprovado](REVISAO_SCHEMA_2026-11-12.md). V1–V5 permanecem intactas.
Foram criadas V6–V12, com 31 constraints, em duas variantes equivalentes. Nenhuma dessas novas
versões foi aplicada ao banco operacional, e nenhum dado real foi modificado para satisfazê-las.

## Arquivos e assuntos

As versões de PostgreSQL ficam em [db/postgresql](../src/main/resources/db/postgresql),
e as de H2 em [db/h2](../src/main/resources/db/h2).
As reversões ficam em [db/rollback/postgresql](../src/main/resources/db/rollback/postgresql)
e [db/rollback/h2](../src/main/resources/db/rollback/h2), fora das locations do Flyway.

| Versão | Assunto | Novas constraints |
| --- | --- | ---: |
| V6 | Textos preenchidos e normalização, mantendo NULL nos campos opcionais | 12 |
| V7 | Login canônico, envelope PBKDF2 e versão de sessão não negativa | 3 |
| V8 | Enum de movimento, sinal da variação e vínculo com OS por tipo | 3 |
| V9 | Técnico após início, conclusão completa, relato e envelope de assinatura | 5 |
| V10 | Formato das chaves/digests e abertura completa ou totalmente legada | 5 |
| V11 | UNIQUE de suporte e FK composta entre chave, movimento e autor | 2 |
| V12 | Consumo único por OS/material; entradas sem OS podem repetir material | 1 |

Cada arquivo de ida executa todas as contagens de seu assunto antes do primeiro ALTER TABLE.
Uma violação causa erro com nome e contagem, por exemplo:

```text
Preflight ck_usuarios_hash_formato: 1 violation(s). No data changed.
```

Não há UPDATE, DELETE, backfill, correção de hashes nem junção de materiais nas migrations.
O diagnóstico é deliberadamente impeditivo: não usar TRY_CAST ou outra conversão que o transforme
em NULL e permita continuar. Mesmo com contagem zero, o ADD CONSTRAINT valida as linhas sob os locks
do banco. Não foi utilizado NOT VALID para tolerar incompatibilidades.

## Seleção de variante e limites

O Spring Boot usa `classpath:db/migration,classpath:db/{vendor}` e resolve o banco da conexão.
Os testes que chamam Flyway diretamente escolhem `db/h2` ou `db/postgresql` explicitamente.
As variantes são diretórios irmãos; nenhuma variante fica sob o diretório comum percorrido
recursivamente. Assim, cada banco carrega V1–V5 e somente uma cópia de V6–V12.

H2 usa REGEXP_LIKE, regexp Java com âncora final estrita, REGEXP_REPLACE global e expressões CASE
equivalentes ao num_nonnulls do PostgreSQL. Foram testados os conjuntos Unicode de isBlank/trim,
inclusive espaços que Java não considera em branco, controles, acentos e caracteres fora do BMP.
Os limites nominais de VARCHAR continuam sujeitos à semântica de caracteres de cada banco e
às unidades UTF-16 do DTO; não foi acrescentada uma regra Unicode diferente para contornar isso.
Referência da sintaxe H2: [funções oficiais](https://h2database.com/html/functions.html#regexp_like).

A imagem continua sendo validada integralmente em Assinatura.validar. A CHECK protege somente
o envelope base64/PNG e o limite textual, sem afirmar que validou pixels ou decodificação completa.
O hash é validado pelo formato do verificador atual; não revela tamanho ou força da senha original.
Uma mudança futura de algoritmo requer revisar a constraint. Nenhuma senha real foi redefinida.

O código continua permitindo material com saldo zero, ajuste negativo que preserve saldo não
negativo, unidades como kg e abertura legada com os três metadados NULL. Não foram adicionados
UNIQUE por descrição, validação de cadastro de fabricante ou triggers de saldo/transição.

## Ensaios e isolamento

```powershell
# H2 em memória; saída isolada do JAR usado por um aplicativo aberto:
.\mvnw.cmd -Dfluxoos.build.directory=target/verificacao verify

# PostgreSQL nativo descartável, sem usar a conexão do .env:
.\scripts\testar-postgres.ps1 -IncluirImportacao
```

O segundo comando precisa dos binários initdb/pg_ctl. Cria um cluster novo em
`target/postgres-test-<uuid>`, restrito a 127.0.0.1, em porta aleatória, com dados sintéticos.
O cluster usa trust apenas nesse ensaio local e é encerrado em finally. Seus arquivos/logs ficam
em target; não há limpeza recursiva de diretórios externos. O build Java desse teste fica em
`target/postgres-verificacao`. A restauração das variáveis de ambiente ocorre mesmo em falha.

O teste de importação PostgreSQL exige `TEST_DATABASE_DISPOSABLE=true` e conexão correspondente
ao cluster de testes; a chamada direta contra o `.env` é recusada. No CI, o marcador é aplicado
ao serviço PostgreSQL descartável. Não apontar as variáveis TEST_DATABASE_* para dados reais.

Cobertura adicionada em [IntegridadeSchemaTest](../src/test/java/br/com/fluxoos/IntegridadeSchemaTest.java):

- Ida de V5 para V12, volta de V12 para V5 e reaplicação, comparando cada valor de todas as sete
  tabelas, incluindo hashes, assinatura, referências e snapshots; somente constraints mudam.
- Contagem das 31 novas constraints e recusa de SQL inválido diretamente pelo banco.
- Login, hashes fora do envelope/faixa, versão negativa, branco/Unicode/trim, limites de texto,
  sinal/enum/OS, conclusão incompleta, chaves e digests.
- Usuário de idempotência diferente do autor do movimento, embora ambas as FKs simples sejam
  válidas, e consumo duplicado com o mesmo par OS/material.
- Um legado inválido por migration, com contagem explícita (inclusive dois registros inválidos),
  abortando sem alterar nenhum registro. O literal hash-preservado é recusado, não saneado.
- Preservação de um hash PBKDF2 válido e dos metadados NULL de OS anterior à V4.

Também foram ensaiados fluxo HTTP completo, concorrência, sessões, idempotência, assinatura,
importação e recuperação da interface. A suíte de interface aceita `FLUXOOS_TEST_JAR` para iniciar
a nova saída em seu próprio H2, sem usar o JAR de um servidor aberto.

## Reversão

Executar as reversões em ordem V12, V11, V10, V9, V8, V7, V6, somente após conferir o estado e as
dependências no ambiente autorizado. V11 remove primeiro a FK composta, depois sua UNIQUE de
suporte. Os arquivos down contêm apenas DROP CONSTRAINT das regras criadas; os dados, as FKs
simples, os CHECKs e as unicidades anteriores permanecem.

O Flyway não executa esses arquivos automaticamente nem baixa a versão ao remover constraints.
Para uma versão já aplicada, usar mecanismo de undo compatível ou, preferencialmente neste projeto,
uma nova migration compensatória com versão superior e os DROP correspondentes. Não excluir
linhas de flyway_schema_history, não executar repair para esconder divergências e não editar
arquivos versionados aplicados. Nos testes de volta, os scripts de ida são reaplicados manualmente
para restaurar o schema antes da validação Flyway, sem manipular seu histórico.

## Estado da entrega

Resultados verificados em 12/11/2026:

| Verificação | Resultado |
| --- | --- |
| Suíte Java completa em H2 em memória | 70 testes, nenhuma falha/erro |
| PostgreSQL 18.6 em cluster descartável | 45 testes, nenhuma falha/erro |
| Interface com o novo JAR V12 e H2 próprio | 19 testes, nenhuma falha |
| Importação, reimportação e conflito transacional com V12 | Aprovado no cluster descartável |
| Build e empacotamento em target/schema-verificacao | BUILD SUCCESS |
| Inspeção das variantes SQL | 7 idas e 7 voltas por banco; 31 ADD/DROP CONSTRAINT; nenhum DML |

Os arquivos estão prontos para revisão. O banco operacional continua sem aplicação de V6–V12;
o servidor existente não foi reiniciado. Iniciar uma versão nova da aplicação fará o Flyway
tentar aplicá-las à conexão configurada, portanto o teste descartável não constitui autorização
para uma implantação operacional ou de produção.

O empacotamento padrão encontrou o JAR em execução e chegou a deixar um intermediário em target.
Esse arquivo foi substituído por um executável compatível com V1–V5, com inicialização verificada
em H2 isolado. A versão com V6–V12 foi empacotada separadamente em
`target/schema-verificacao/fluxoos-1.0.0-SNAPSHOT.jar`; o servidor existente não foi reiniciado.
