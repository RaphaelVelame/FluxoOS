# Estoques por proprietário

O estoque pode ser separado entre empresa e clientes. Cada cadastro tem código único, nome e tipo
(`EMPRESA` ou `CLIENTE`), e cada material pode pertencer a um desses estoques. Não são criadas
tabelas diferentes para cada cliente.

## Utilização

1. Em **Estoque**, clique em **Novo estoque** e cadastre a empresa/unidade ou o cliente proprietário.
2. Ao cadastrar um material, selecione seu estoque. Para o mesmo produto em estoques distintos,
   use cadastros de material com códigos únicos distintos; descrições podem ser iguais.
3. Use **Tipo de proprietário** para alternar entre Empresa, Clientes e Sem classificação, e
   **Estoque / cliente** para escolher um cadastro específico. A busca também encontra seu nome.
4. Para os materiais antigos, escolha **Sem classificação** e use **Vincular a um estoque**,
   ou selecione até 500 materiais e clique em **Vincular selecionados**.
5. Confira o estoque informado nos cartões, no seletor de material da OS e no histórico.

Os 169 materiais já cadastrados permanecerão sem classificação até que o usuário identifique os
proprietários, conforme a escolha expressa nesta conversa. A migration não atribui nenhum deles
à empresa ou a um cliente. O importador continua criando materiais sem classificação, porque a
planilha atual não informa propriedade.

Se o material tem `estoqueId`, mas o cadastro desse estoque ainda não está disponível na consulta,
a interface mostra **Estoque #ID** e o contabiliza como **Estoque a confirmar**. Somente referências
NULL aparecem como **Sem classificação** e permitem classificação pela tela.

Chefia e suporte TI podem cadastrar estoques e classificar materiais. A consulta mantém os perfis
de acesso existentes da aplicação; o filtro por cliente organiza a propriedade, sem criar um novo
perfil de acesso externo.

## Saldos e histórico

O saldo continua no cadastro de cada material. Movimentar ou consumir um material do cliente não
altera o saldo de outro cadastro pertencente à empresa. Não há soma automática de materiais de
mesma descrição nem mistura de unidades.

Classificar um material sem proprietário não transfere quantidades, não gera entrada/saída e não
altera movimentos ou itens anteriores. Os novos movimentos e consumos guardam o `estoqueId` da
época. Por isso, o histórico antigo pode continuar com **Sem classificação** mesmo que o cadastro
atual já esteja identificado. A auditoria da classificação registra os IDs, códigos e saldo atual.

Repetir a classificação para o mesmo estoque é seguro. Tentar vincular um material já classificado
a outro proprietário cancela a transação; uma transferência de propriedade/quantidade precisa de
um fluxo próprio, não de uma troca silenciosa do cadastro. Esse fluxo não foi criado nesta entrega.

A OS atual não possui um cadastro estruturado de cliente. O sistema identifica a origem do material
escolhido, mas não infere o proprietário pelo texto do solicitante nem restringe automaticamente
o consumo ao cliente da OS.

## Banco de dados — V13

- Nova tabela `estoques`: PK, código único, tipo restrito a empresa/cliente, campos obrigatórios,
  texto preenchido e normalizado.
- Referência nullable `estoque_id` em `materiais`, `movimentos` e `itens_os`, com FKs para `estoques`.
  `NULL` preserva o legado desconhecido, sem backfill nem proprietário presumido.
- UNIQUE de suporte `(materiais.id, materiais.estoque_id)` e FKs compostas nos movimentos/itens
  impedem que um estoque informado aponte para outro proprietário do material. Os snapshots NULL
  legados permanecem permitidos por desenho; o serviço sempre captura o proprietário atual ao gravar.
- Índices de estoque nos materiais e movimentos. Códigos de material mantêm sua unicidade global.

Variantes: [PostgreSQL](../src/main/resources/db/postgresql/V13__propriedade_estoque.sql) e
[H2](../src/main/resources/db/h2/V13__propriedade_estoque.sql). Antes de adicionar as regras, a migration
executa as contagens e aborta em inconsistência. A tabela nova começa vazia; as novas referências
começam NULL. Os ensaios confirmam zero violações na atualização dos dados legados de teste.

As reversões ficam em `db/rollback/<banco>/V13__propriedade_estoque.down.sql`. Elas só removem o
schema novo quando não existe nenhum cadastro ou vínculo de propriedade. Se houver informação
nova, abortam antes do DDL com a contagem encontrada, preservando tudo. Não executar DELETE ou
limpar vínculos para forçar essa reversão; uma volta com dados exige planejar sua preservação.
Os arquivos down não são executados automaticamente pelo Flyway e seu histórico não deve ser apagado.

A reversão deve ocorrer em manutenção, com as gravações da aplicação interrompidas. A variante
PostgreSQL inclui transação e locks das quatro tabelas para evitar gravações entre a contagem e
o DROP. Se uma execução interativa falhar, faça ROLLBACK para liberar a transação; ao executar por
psql, use ON_ERROR_STOP. H2 faz commits de DDL e exige exclusividade operacional durante a volta;
não presumir rollback transacional de DDL nesse banco.
Se a volta for transformada em migration compensatória, revisar os comandos para que o Flyway
controle a transação; não copiar BEGIN/COMMIT do script manual sem essa adaptação.

## Validação e implantação

Os testes usam H2 em memória e um cluster PostgreSQL descartável próprio. Cobrem FKs, códigos,
tipos, separação de saldo/consumo, permissões, repetição segura, rollback de lotes, preservação
de históricos e reversão com/sem dados novos. A interface é ensaiada em desktop e celular.

Resultados verificados em 12/11/2026: 77 testes Java em H2, 52 em PostgreSQL descartável e 22 de
interface aprovados. Após o refinamento visual e de foco, os três fluxos novos de interface foram
retestados e aprovados. A reversão com locks também foi revalidada no PostgreSQL, e o build foi
concluído em diretório separado. Nenhum ensaio usou dados operacionais.

```powershell
.\mvnw.cmd -Dfluxoos.build.directory=target/estoques-verificacao verify
.\scripts\testar-postgres.ps1 -IncluirImportacao
```

O JAR dessa funcionalidade fica em `target/estoques-verificacao/fluxoos-1.0.0-SNAPSHOT.jar`.
Não foi reiniciado o servidor atual nem aplicada V13 ao banco operacional. Atualizar a aplicação
exige uma execução controlada das migrations pendentes (incluindo V6–V12, se ainda ausentes).
