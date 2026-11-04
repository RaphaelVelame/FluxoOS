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

`.env.example` documenta a configuração. `.env` é lido pelo Docker Compose; para Java exporte as variáveis ou use um gerenciador de segredos do seu ambiente. Nunca versione credenciais, banco ou backups.

## Uso em celular

A interface adapta-se ao navegador móvel. Para acessá-la em um celular físico, execute em rede confiável com `SERVER_ADDRESS=0.0.0.0` e use o IP da máquina no navegador do celular. Configure o firewall e HTTPS conforme o ambiente. O padrão fica restrito à própria máquina. A aplicação requer conexão ao servidor; não há cache offline de dados ou sincronização em segundo plano.

## Antes de implantação

- Use PostgreSQL e ensaie o fluxo e as migrações no banco de destino. Os testes automatizados fornecidos usam H2; não certificam sozinhos a operação PostgreSQL.
- Configure HTTPS, origem e cabeçalhos de proxy confiáveis. Use `COOKIE_SECURE=true`. O filtro compara Origin com o endereço efetivamente percebido pelo servidor; configure corretamente o proxy antes de disponibilizar a aplicação.
- Remova a senha inicial do ambiente após o primeiro provisionamento. Não use dados demonstrativos em produção.
- A implementação de sessão e limite de login é por JVM. Use uma única instância enquanto não houver armazenamento distribuído. Reiniciar encerra as sessões.
- Defina backup, restauração, retenção de auditoria e assinatura, proteção do banco, limites de tráfego e monitoramento no ambiente.

## Backup e restauração

Para H2 local, encerre o processo Java com Ctrl+C e só então copie `data/fluxoos.mv.db` para uma pasta protegida. Não copie o arquivo enquanto estiver aberto. Para restaurar, pare o aplicativo e restaure uma cópia compatível nesse caminho; preserve uma cópia do banco atual antes de substituí-lo.

Para PostgreSQL, use `pg_dump` em formato customizado e `pg_restore` em um banco de teste antes de assumir que o backup está válido. Agendamento e armazenamento são responsabilidades do ambiente; não há tarefa de backup automática embutida.

## Verificação

`mvnw verify` compila, executa os testes e empacota o JAR. A aplicação está pronta para teste local quando o log mostra `Started FluxoOsApplication` e a página inicial abre. Os testes não dependem de uma porta fixa nem do banco de desenvolvimento. Migrações já aplicadas não devem ser editadas; acrescente arquivos `V2__...sql`, `V3__...sql` etc.

O endpoint de assinatura e as consultas da equipe seguem as permissões de consulta do README. Se os atendimentos precisarem de segregação por unidade, organização ou solicitante, implemente esse filtro no servidor antes de incluir dados reais desse escopo.
