# FluxoOS

Aplicação web de ordens de serviço e estoque, com Java 25, Spring Boot, Spring Data JPA/Hibernate e interface **mobile first** em HTML, CSS e JavaScript.

O backend segue uma estrutura simples: **controller → service → repository**, com entidades JPA, DTOs, configuração, enums e segurança em pacotes separados. Este README reúne a documentação atual do projeto.

## Executar no Windows

Requer **JDK 25 ou superior** e conexão à internet na primeira compilação. O Maven Wrapper baixa o Maven e as dependências oficiais; não é necessário instalar Maven ou Node. A integração local usa PostgreSQL quando o perfil `postgres` está configurado no `.env`.

```powershell
.\scripts\executar.ps1
```

O script carrega o `.env` da raiz, preservando a prioridade das variáveis já definidas no terminal, e verifica a conexão PostgreSQL antes de iniciar. Na primeira execução de um banco sem usuários, pede uma senha de pelo menos 15 caracteres para a conta **`admin`**. Abra **http://localhost:8080** após a mensagem `Started FluxoOsApplication`. Para usar H2 explicitamente, execute `./scripts/executar.ps1 -H2`.

Para iniciar uma base nova com materiais, três ordens e uma conta `tecnico` de demonstração:

```powershell
.\scripts\executar.ps1 -Demo
```

As contas demonstrativas usam a senha inicial definida por você. A carga ocorre **somente se ainda não houver usuários**. O parâmetro não apaga nem modifica uma base existente.

Alternativa manual e Linux/macOS:

```bash
export BOOTSTRAP_PASSWORD='defina-uma-senha-longa-e-unica'
# Opcional, apenas para uma base nova de demonstração:
export DEMO_DATA=true
sh ./mvnw spring-boot:run
```

Após a criação da conta, remova a variável de senha do ambiente. Para mudar a senha use **Minha conta**. Não existe senha fixa de fábrica ou cadastro público.

## Funcionalidades

- Login com PBKDF2, limitação por usuário/IP, cookie HttpOnly/SameSite, proteção CSRF e troca de senha com revogação de sessões.
- Painel com ordens abertas, em andamento, concluídas e alertas de estoque.
- Abertura de OS, filtros, atribuição e reatribuição de técnico, início, relato e histórico.
- Cadastro de materiais, entradas, devoluções, ajustes positivos/negativos e ativação/desativação.
- Consumo dentro da OS: atualização de saldo, item, movimento, auditoria e chave de idempotência na **mesma transação JPA**.
- Conclusão com relato obrigatório, nome do responsável e assinatura PNG capturada por dedo, caneta ou mouse e persistida no banco.
- Gestão de contas pela chefia, desativação de acesso e consulta de eventos de segurança.
- Layout para celular com navegação inferior, sidebar em telas maiores, formulários acessíveis, estados vazios e mensagens de erro.

## Permissões implementadas

Por padrão, a aplicação admite 20 sessões ativas no total por instância. Cinco erros por usuário em 15 minutos bloqueiam novos logins por cinco minutos; há proteção adicional por IP e descarte automático dos registros antigos. A 21ª sessão é recusada sem encerrar as existentes. Consulte [configuração e operação](docs/OPERACAO.md) e [testes da correção](docs/CORRECAO_LIMITES_2026-11-11.md).

| Operação | Chefia | Suporte TI | Técnico | Solicitante |
| --- | :---: | :---: | :---: | :---: |
| Consultar ordens, estoque e diretório da equipe | Sim | Sim | Sim | Sim |
| Abrir OS e atribuir técnico | Sim | Sim | Não | Não |
| Atender, relatar, consumir e concluir OS | Sim | Sim | Apenas quando designado | Não |
| Cadastrar/movimentar/desativar material | Sim | Sim | Não | Não |
| Criar/desativar contas e consultar eventos de segurança | Sim | Não | Não | Não |
| Trocar a própria senha | Sim | Sim | Sim | Sim |

Essas verificações ocorrem no servidor. Não é possível iniciar atendimento sem técnico designado, consumir material inativo ou sem saldo, registrar duas vezes o mesmo material na OS, nem alterar uma OS concluída. Correções de consumo ainda requerem um fluxo específico; devoluções manuais ficam no histórico do estoque.

## Organização

```text
src/main/java/br/com/fluxoos/
  controller/         Endpoints REST e tratamento de erros
  entity/             Entidades JPA e validações dos dados
  dto/                Dados de entrada e saída da API
  service/            Regras de negócio, autenticação e transações
  config/             Configuração Spring, filtro HTTP e carga inicial
  repository/         Repositories Spring Data JPA e armazenamento de sessões
  enums/              Perfil, Prioridade, Situacao, TipoMovimento e TipoEventoSeguranca
  seguranca/          PBKDF2, tokens, sessão e controle de tentativas
src/main/resources/
  application.yml     Configuração padrão (H2)
  application-postgres.yml  Configuração do perfil PostgreSQL
  db/migration/       Migrações Flyway
  static/
    css/              Tokens visuais e estilos responsivos
    js/views/         Telas de painel, OS, estoque, equipe e conta
    js/               API, componentes, navegação e captura de assinatura
src/test/java/        Testes de integração, segurança, carga inicial e estrutura
docs/                 Arquitetura, API e operação
scripts/              Execução e testes
```

Os controllers recebem requisições e chamam os services. Os services aplicam as regras e delimitam as transações com `@Transactional`. Os repositories estendem `JpaRepository` e persistem as entidades anotadas com `@Entity`; os itens da OS são mapeados com `@ElementCollection`. DTOs preservam o formato da API e evitam expor hashes, assinaturas nas listagens e objetos internos do Hibernate.

A organização não usa arquitetura hexagonal, portas/adaptadores nem uma camada de views MVC no backend. A interface estática consome a API REST. O Spring Web continua fornecendo os controllers HTTP.

## Dados e PostgreSQL

Com o perfil **`postgres`**, os dados ficam no PostgreSQL configurado por `DATABASE_URL`, `DATABASE_USER` e `DATABASE_PASSWORD`. Na instalação local integrada, o banco e o usuário da aplicação se chamam **`fluxoos`**, no servidor **`localhost:5432`**. O usuário da aplicação não tem privilégios de superusuário, criação de bancos ou criação de papéis. A senha de conexão fica no `.env`, ignorado pelo Git.

Sem o perfil PostgreSQL, a execução direta usa **`data/fluxoos.mv.db`** (H2 em arquivo). A opção `-H2` do script também seleciona essa base. Os dois bancos preservam os dados após reiniciar e são independentes.

O Flyway cria e versiona o esquema, e o Hibernate valida os mapeamentos com `ddl-auto: validate`. O projeto fixa Flyway 11.20.0 para reconhecer PostgreSQL 18, conforme o [módulo oficial dessa versão](https://github.com/flyway/flyway/blob/flyway-11.20.0/flyway-database/flyway-database-postgresql/src/main/java/org/flywaydb/database/postgresql/PostgreSQLDatabase.java#L47), mantendo a mesma versão nos módulos principal e PostgreSQL. A migração V1 foi preservada para as bases existentes; a V2 acrescenta o alvo dos eventos de segurança. JPA/Hibernate fazem o acesso aos dados. As URLs de conexão continuam com o prefixo `jdbc:`, exigido pelos drivers H2 e PostgreSQL.

Para usar outra instalação nativa do PostgreSQL, crie um banco e um usuário próprios para a aplicação, copie `.env.example` para `.env` e ajuste a conexão. No Windows:

```powershell
.\scripts\executar.ps1
```

O script encontra `psql` no `PATH` ou em `C:\Program Files\PostgreSQL\<versão>\bin`. A senha inicial do `admin` da aplicação é independente da senha do banco. Para execução local por HTTP, o script usa `COOKIE_SECURE=false` quando essa opção não foi definida. A execução direta do perfil `postgres` continua exigindo cookies HTTPS por padrão.

Como alternativa, use Docker Compose, desde que a porta 5432 esteja livre:

```powershell
$env:DATABASE_PASSWORD = 'defina-uma-senha-do-banco'
docker compose up -d
$env:SPRING_PROFILES_ACTIVE = 'postgres'
$env:DATABASE_URL = 'jdbc:postgresql://localhost:5432/fluxoos'
$env:DATABASE_USER = 'fluxoos'
# Apenas para desenvolvimento HTTP local:
$env:COOKIE_SECURE = 'false'
$env:BOOTSTRAP_PASSWORD = 'defina-a-senha-do-admin-no-novo-banco'
.\mvnw.cmd spring-boot:run
```

Cada banco tem sua própria carga inicial. Trocar a URL não transfere os dados H2. O Compose inicia somente o banco; execute o backend separadamente. `mvnw` e `java -jar` não carregam `.env` automaticamente; exporte as variáveis ao usá-los diretamente.

## Testes e distribuição

```powershell
.\scripts\testar.ps1
.\scripts\testar-postgres.ps1
.\mvnw.cmd verify
java -jar target/fluxoos-1.0.0-SNAPSHOT.jar
```

No Linux/macOS, use `sh ./scripts/testar.sh` e `sh ./mvnw verify`. A suíte padrão usa H2 em memória e não altera a base local. Cobre fluxo HTTP completo, autenticação/CSRF, permissões, transações JPA com rollback real, concorrência de consumo, idempotência, assinatura, carga demonstrativa e separação dos controllers.

Os [testes de interface](tests/e2e/README.md) verificam gravação confirmada seguida de falha de consulta, perda de resposta e recuperação sem reenvio. Usam Node 24 e Playwright somente para testes, iniciando um servidor próprio com H2 em memória. O CI também executa essa suíte. A abertura de OS pela API agora exige `Idempotency-Key`, conforme o [contrato HTTP](docs/API.md).

`testar-postgres.ps1` carrega a conexão do `.env`, cria um esquema temporário com nome aleatório, aplica as migrações e executa os mesmos testes operacionais no PostgreSQL real. O esquema é removido ao final, inclusive quando os testes falham; as tabelas do esquema `public` permanecem preservadas. A URL de conexão para esse script deve estar sem parâmetros. Uma interrupção forçada do processo pode exigir a remoção manual do esquema `fluxoos_test_...` deixado para trás. O GitHub Actions executa `verify` com Java 25 e repete os testes operacionais em um PostgreSQL 18 descartável.

Consulte [arquitetura](docs/ARQUITETURA.md), [API](docs/API.md) e [operação](docs/OPERACAO.md).

## Limites da entrega

Esta é uma aplicação funcional para execução local e evolução, sem publicação automática. A integração inclui validação das migrações e do fluxo operacional em PostgreSQL real; repita essa verificação ao trocar o ambiente de destino. Sessões e limites de login continuam em memória: reiniciar exige novo login, e múltiplas instâncias precisam de armazenamento compartilhado. Backups, HTTPS, monitoração externa, recuperação de desastre e políticas de retenção precisam ser configurados no ambiente de implantação. A assinatura é uma imagem do atendimento; não implementa certificação digital. Não há envio de e-mails, recuperação de senha, anexos, modo offline ou aplicativo nativo.
