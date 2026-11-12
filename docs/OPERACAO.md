# Operação local e implantação

## Configuração

| Variável | Padrão | Uso |
| --- | --- | --- |
| `PORT` | `8080` | Porta HTTP |
| `SERVER_ADDRESS` | `127.0.0.1` | Endereço de escuta |
| `DATABASE_URL` | H2 em `./data/fluxoos` | URL JDBC |
| `DATABASE_USER` | `sa` (H2) / `fluxoos` (perfil postgres) | Usuário SQL |
| `DATABASE_PASSWORD` | vazio no H2 | Senha SQL |
| `SPRING_PROFILES_ACTIVE` | vazio | Use `postgres` para PostgreSQL |
| `BOOTSTRAP_PASSWORD` | vazio | Obrigatória somente em banco sem usuários |
| `DEMO_DATA` | `false` | Carga demonstrativa somente em banco vazio |
| `COOKIE_SECURE` | `false` local / `true` postgres | Cookie somente por HTTPS |
| `MAX_SESSOES` | `20` | Sessões ativas no total por JVM; excedente recebe 429 |
| `LOGIN_MAX_ERROS` | `5` | Erros por usuário para bloquear novos logins |
| `LOGIN_MAX_ERROS_IP` | `30` | Erros por IP para bloqueio adicional |
| `LOGIN_JANELA_MINUTOS` | `15` | Janela de contagem das falhas |
| `LOGIN_BLOQUEIO_MINUTOS` | `5` | Prazo de bloqueio, sem extensão por tentativas recusadas |
| `LOGIN_MAX_IDENTIFICADORES` | `10000` | Capacidade de cada mapa: nomes e IPs |
| `SEGURANCA_LIMPEZA_MS` | `60000` | Intervalo da manutenção de sessões e tentativas |

`application.yml` contém a configuração padrão, e `application-postgres.yml` contém as opções do perfil `postgres`. O acesso a dados usa Spring Data JPA/Hibernate, com validação do esquema criado pelo Flyway. `.env.example` documenta as variáveis. `scripts/executar.ps1` carrega o `.env`, com prioridade para variáveis já exportadas, e restaura o ambiente do terminal ao terminar. O arquivo aceita linhas `NOME=valor` e valores entre aspas simples ou duplas; não executa comandos nem expande variáveis. Docker Compose também lê `.env`; para Java/Maven diretamente, exporte as variáveis ou use um gerenciador de segredos. Nunca versione credenciais, banco ou backups.

## Sessões e bloqueios

O limite de sessões é global por instância, não por conta. Ao atingir 20, uma nova entrada é recusada sem derrubar quem já está conectado. Fechar o navegador não avisa o servidor: a sessão é descartada após 30 minutos de inatividade ou oito horas absolutas; logout libera a vaga imediatamente. Desativação e troca de senha continuam invalidando sessões pela versão da conta. A manutenção as remove em até um ciclo, e uma nova admissão também faz essa limpeza. A consulta de estado usada pelo descarte lê somente alterações confirmadas, preservando sessões em rollback.

Os mapas de tentativas descartam registros sem falhas nem bloqueio vigente. Se a capacidade estiver toda ocupada, identidades novas são temporariamente recusadas até liberar espaço; bloqueios ativos não são expulsos para abrir vaga. Credenciais válidas removem o contador do usuário, mas não zeram o agregado de IP. Reiniciar a JVM encerra sessões e limpa os contadores; múltiplas instâncias precisam de armazenamento compartilhado para impor um limite único na implantação inteira. A retenção dos eventos de auditoria no banco permanece uma política operacional separada.

## PostgreSQL local

A instalação local utiliza PostgreSQL 18.6 em `localhost:5432`, banco `fluxoos` e usuário `fluxoos`. Esse usuário possui o banco e pode criar as tabelas do Flyway, sem privilégios administrativos sobre o servidor. O `.env` contém o perfil `postgres` e a senha exclusiva do usuário da aplicação; a senha do superusuário `postgres` não é necessária para a execução.

Execute `./scripts/executar.ps1`. O script encontra `psql` no `PATH` ou na instalação padrão do Windows, verifica a conexão e consulta se há usuários antes de solicitar a senha inicial do `admin`. A verificação usa o PostgreSQL selecionado, independentemente da existência de um arquivo H2 antigo. `-Demo` permite carga demonstrativa apenas em banco vazio. `-H2` seleciona o banco H2 explicitamente. Não há transferência automática de dados entre os bancos.

Na execução pelo script, cookies usam HTTP local por padrão, salvo `COOKIE_SECURE` explícito. Em acesso HTTPS, defina `COOKIE_SECURE=true`. A execução direta do perfil PostgreSQL mantém o padrão seguro `true`.

## Uso em celular

A interface adapta-se ao navegador móvel. Para acessá-la em um celular físico, execute em rede confiável com `SERVER_ADDRESS=0.0.0.0` e use o IP da máquina no navegador do celular. Configure o firewall e HTTPS conforme o ambiente. O padrão fica restrito à própria máquina. A aplicação requer conexão ao servidor; não há cache offline de dados ou sincronização em segundo plano.

## Antes de implantação

- Use PostgreSQL e ensaie o fluxo e as migrações no banco de destino com `./scripts/testar-postgres.ps1`. Ele cria um esquema isolado temporário e executa os testes de fluxo HTTP, rollback, concorrência, idempotência e assinatura contra o PostgreSQL real.
- Configure HTTPS, origem e cabeçalhos de proxy confiáveis. Use `COOKIE_SECURE=true`. O filtro compara Origin com o endereço efetivamente percebido pelo servidor; configure corretamente o proxy antes de disponibilizar a aplicação.
- Remova a senha inicial do ambiente após o primeiro provisionamento. Não use dados demonstrativos em produção.
- A implementação de sessão e limite de login é por JVM. Use uma única instância enquanto não houver armazenamento distribuído. Reiniciar encerra as sessões.
- Defina backup, restauração, retenção de auditoria e assinatura, proteção do banco, limites de tráfego e monitoramento no ambiente.

## Backup e restauração

Para H2 local, encerre o processo Java com Ctrl+C e só então copie `data/fluxoos.mv.db` para uma pasta protegida. Não copie o arquivo enquanto estiver aberto. Para restaurar, pare o aplicativo e restaure uma cópia compatível nesse caminho; preserve uma cópia do banco atual antes de substituí-lo.

Para PostgreSQL, use `pg_dump` em formato customizado e `pg_restore` em um banco de teste antes de assumir que o backup está válido. Agendamento e armazenamento são responsabilidades do ambiente; não há tarefa de backup automática embutida.

## Verificação

`mvnw verify` compila, executa os testes e empacota o JAR. A aplicação está pronta para teste local quando o log mostra `Started FluxoOsApplication` e a página inicial abre. Os testes não dependem de uma porta fixa nem do banco de desenvolvimento. Migrações já aplicadas não devem ser editadas; acrescente arquivos `V2__...sql`, `V3__...sql` etc.

`scripts/testar-postgres.ps1` usa `DATABASE_URL` sem parâmetros e credenciais de um usuário com permissão de criar esquemas no banco. Cada execução cria `fluxoos_test_<identificador aleatório>` e o remove em `finally`. A aplicação continua usando suas tabelas em `public`. Não defina `TEST_DATABASE_URL` manualmente para um banco com dados: o teste operacional limpa as tabelas do esquema escolhido. No CI, essas variáveis apontam exclusivamente para um banco descartável do serviço PostgreSQL.

O endpoint de assinatura e as consultas da equipe seguem as permissões de consulta do README. Se os atendimentos precisarem de segregação por unidade, organização ou solicitante, implemente esse filtro no servidor antes de incluir dados reais desse escopo.
