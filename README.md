# FluxoOS

Aplicação web de ordens de serviço e estoque, com backend Java e interface **mobile first**. O código original de domínio e segurança foi reaproveitado e reorganizado por responsabilidade.

O [README original](README%20(2).md) é a referência funcional inicial. Este arquivo descreve o que está implementado e como executar a versão atual.

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
- Consumo dentro da OS: atualização de saldo, item, movimento, auditoria e chave de idempotência na **mesma transação SQL**.
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
  domain/
    modelo/           Entidades, enums e regras originais de OS/estoque
    seguranca/        Sessão, tokens e eventos de segurança
  application/
    porta/            Contratos dos repositórios e do hash de senha
    seguranca/        Autenticação, sessões, troca de senha e controle de tentativas
    ...Service.java   Casos de uso transacionais
  infrastructure/
    config/           Composição de dependências e carga inicial
    persistencia/     Adaptadores JDBC
    memoria/          Sessões e adaptadores originais úteis em testes
    seguranca/        Implementação PBKDF2
    web/              API, validação, proteção HTTP e respostas de erro
src/main/resources/
  db/migration/       Migrações Flyway
  static/
    css/              Tokens visuais e estilos responsivos
    js/views/         Telas de painel, OS, estoque, equipe e conta
    js/               API, componentes, navegação e captura de assinatura
src/test/java/        Testes de integração e isolamento do domínio
docs/                 Arquitetura, API e operação
scripts/              Execução e testes
```

O domínio não importa Spring, HTTP, JDBC ou classes de infraestrutura. Serviços de aplicação coordenam os casos de uso através de portas. JDBC implementa essas portas. Os controladores traduzem requisições e respostas, usando DTOs que não expõem hashes ou assinaturas nas listagens. Não foram criadas camadas vazias, interfaces por classe ou um framework de frontend sem necessidade.

## Dados e PostgreSQL

Por padrão, os dados ficam em **`data/fluxoos.mv.db`** (H2 em arquivo), preservados após reiniciar o servidor. A pasta é ignorada pelo Git. O Flyway cria e versiona o esquema.

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

No Linux/macOS, use `sh ./scripts/testar.sh` e `sh ./mvnw verify`. Os testes usam H2 em memória e não alteram a base local. Cobrem fluxo HTTP completo, autenticação/CSRF, permissões, transações com rollback real, concorrência de consumo, idempotência, assinatura e arquitetura. O workflow GitHub Actions executa `verify` com Java 17.

Consulte [arquitetura](docs/ARQUITETURA.md), [API](docs/API.md) e [operação](docs/OPERACAO.md).

## Limites da entrega

Esta é uma aplicação funcional para execução local e evolução, sem publicação automática. A configuração PostgreSQL está disponível, mas precisa ser validada contra sua instância antes de implantação. Sessões e limites de login continuam em memória: reiniciar exige novo login, e múltiplas instâncias precisam de armazenamento compartilhado. Backups, HTTPS, monitoração externa, recuperação de desastre e políticas de retenção precisam ser configurados no ambiente de implantação. A assinatura é uma imagem do atendimento; não implementa certificação digital. Não há envio de e-mails, recuperação de senha, anexos, modo offline ou aplicativo nativo.
