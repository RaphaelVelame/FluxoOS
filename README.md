# FluxoOS

Aplicação web de ordens de serviço e estoque, com Java 17, Spring Boot, Spring Data JPA/Hibernate e interface **mobile first** em HTML, CSS e JavaScript.

O backend segue uma estrutura simples: **controller → service → repository**, com entidades JPA, DTOs, configuração, enums e segurança em pacotes separados. Este README reúne a documentação atual do projeto.

## Executar no Windows

Requer **JDK 17 ou superior** e conexão à internet na primeira compilação. O Maven Wrapper baixa o Maven e as dependências oficiais; não é necessário instalar Maven, Node ou um banco externo para começar.

```powershell
.\scripts\executar.ps1
```

Na primeira execução, o script pede uma senha de pelo menos 15 caracteres para a conta **`admin`**. Abra **http://localhost:8080** após a mensagem `Started FluxoOsApplication`.

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

Por padrão, os dados ficam em **`data/fluxoos.mv.db`** (H2 em arquivo), preservados após reiniciar o servidor. A pasta é ignorada pelo Git. Toda a configuração da aplicação está em **YAML**; variáveis de ambiente continuam disponíveis.

O Flyway cria e versiona o esquema, e o Hibernate valida os mapeamentos com `ddl-auto: validate`. A migração V1 foi preservada para as bases existentes; a V2 acrescenta o alvo dos eventos de segurança. JPA/Hibernate fazem o acesso aos dados. As URLs de conexão continuam com o prefixo `jdbc:`, exigido pelos drivers H2 e PostgreSQL.

Para PostgreSQL, instale Docker ou disponibilize seu próprio banco. Com Docker Compose:

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

Cada banco tem sua própria carga inicial. Trocar a URL não transfere os dados H2. O Compose inicia somente o banco; execute o backend separadamente.

## Testes e distribuição

```powershell
.\scripts\testar.ps1
.\mvnw.cmd verify
java -jar target/fluxoos-1.0.0-SNAPSHOT.jar
```

No Linux/macOS, use `sh ./scripts/testar.sh` e `sh ./mvnw verify`. Os testes usam H2 em memória e não alteram a base local. Cobrem fluxo HTTP completo, autenticação/CSRF, permissões, transações JPA com rollback real, concorrência de consumo, idempotência, assinatura, carga demonstrativa e separação dos controllers. O workflow GitHub Actions executa `verify` com Java 17.

Consulte [arquitetura](docs/ARQUITETURA.md), [API](docs/API.md) e [operação](docs/OPERACAO.md).

## Limites da entrega

Esta é uma aplicação funcional para execução local e evolução, sem publicação automática. A configuração PostgreSQL está disponível, mas precisa ser validada contra sua instância antes de implantação. Sessões e limites de login continuam em memória: reiniciar exige novo login, e múltiplas instâncias precisam de armazenamento compartilhado. Backups, HTTPS, monitoração externa, recuperação de desastre e políticas de retenção precisam ser configurados no ambiente de implantação. A assinatura é uma imagem do atendimento; não implementa certificação digital. Não há envio de e-mails, recuperação de senha, anexos, modo offline ou aplicativo nativo.
