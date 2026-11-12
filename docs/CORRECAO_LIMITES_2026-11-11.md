# Correção: capacidade e descarte da autenticação

Implementada em 11/11/2026. A implementação anterior aceitava mais de 20 sessões, conservava sessões e identidades antigas indefinidamente e bloqueava o usuário por 15 minutos. Os dois primeiros testes HTTP reproduziram a ausência do limite e a duração incompatível antes da correção.

## Política aplicada

| Controle | Comportamento padrão |
| --- | --- |
| Sessões simultâneas | 20 no total por instância/JVM, somando todas as contas |
| 21ª sessão | Recusada com 429; preserva as 20 existentes |
| Logout | Revoga o token e libera a vaga imediatamente |
| Inatividade | Expira após 30 minutos sem uso |
| Limite absoluto | Expira após oito horas, mesmo com atividade |
| Falhas por usuário | Cinco erros em uma janela de 15 minutos |
| Bloqueio por usuário | Cinco minutos a partir do erro que atingiu o limite |
| Falhas por IP | 30 erros na mesma janela; bloqueio adicional de cinco minutos |
| Tentativas durante o bloqueio | Recusadas sem estender o prazo |
| Tamanho dos mapas | Até 10.000 nomes e 10.000 IPs |
| Manutenção | A cada 60 segundos, além da limpeza no acesso/admissão |

O escopo global foi adotado para a aplicação atual de uma instância. Não é um limite de 20 por usuário. Fechar uma aba ou navegador não equivale a logout; o servidor aguarda inatividade ou expiração se não receber a saída.

O bloqueio segue o nome normalizado mesmo trocando de IP. Credenciais válidas zeram o contador do usuário, mas não o contador agregado do IP. Sessões já abertas não são encerradas por erros de senha em novas tentativas. Login incorreto e bloqueio preservam a resposta uniforme 401 para evitar revelar existência de conta; o 429 de capacidade só ocorre depois da verificação das credenciais.

## Pesquisa e escolha

Foram consultados:

- [OWASP — Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html): limiar, janela de observação e duração devem ser definidos separadamente; proteção por conta deve considerar também ataques distribuídos.
- [Spring Security — Session Management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html): opção de preservar sessões existentes e impedir novas entradas ao alcançar capacidade.
- [Caffeine — Eviction no GitHub](https://github.com/ben-manes/caffeine/wiki/Eviction): limitação por tamanho, expiração por tempo e manutenção sem depender de tráfego.
- [Spring Framework — Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html): execução periódica com `@Scheduled`.

Cinco erros/cinco minutos é a política solicitada, não um valor universal obrigatório prescrito pela OWASP. Foi mantida uma janela de 15 minutos e um limiar de IP maior para reduzir bloqueios coletivos em redes compartilhadas. Os valores são configuráveis.

Não foi adicionado um cache com expulsão automática por recência: substituir uma entrada de bloqueio ainda válida permitiria perdê-la sob pressão de novas identidades. Foram mantidos mapas pequenos e limitados, com admissão sincronizada e descarte somente de controles inativos. Na saturação, identidades novas são recusadas até expirar/liberar espaço, preservando os bloqueios existentes.

## Implementação

`SessoesEmMemoria` verifica capacidade e grava sob o mesmo monitor. A limpeza remove sessões revogadas ou expiradas antes da inclusão, consulta e listagem. Logout remove o registro. As listagens devolvem cópias, não o mapa mutável.

Antes da admissão de uma sessão, e durante a manutenção, `SessaoService` busca em lote o estado dos usuários das sessões existentes. Remove tokens associados a usuários ausentes, inativos ou com outra versão de sessão. A consulta usa `REQUIRES_NEW` e leitura somente de dados confirmados: uma desativação ou troca de senha ainda não confirmada não provoca descarte permanente em rollback. O acesso ao banco ocorre fora do bloqueio do mapa.

O limitador cria entradas somente ao registrar falhas. Consultar nomes/IPs desconhecidos não os retém no mapa. Controles sem falhas na janela nem bloqueio ativo são descartados. O descarte não antecipa o fim de um bloqueio, mesmo se sua duração for maior que a janela de observação. Contadores por identidade também param de acumular falhas enquanto bloqueados.

`ManutencaoSeguranca` usa `@Scheduled` e limpa tentativas e sessões mesmo sem novas requisições. A admissão continua protegida por capacidade durante execução concorrente da limpeza. A política pode ser ajustada no ambiente ou no `.env` carregado por `scripts/executar.ps1`; variáveis já exportadas continuam tendo prioridade.

## Configuração

Os padrões estão em `application.yml` e `.env.example`: `MAX_SESSOES=20`, `LOGIN_MAX_ERROS=5`, `LOGIN_MAX_ERROS_IP=30`, `LOGIN_JANELA_MINUTOS=15`, `LOGIN_BLOQUEIO_MINUTOS=5`, `LOGIN_MAX_IDENTIFICADORES=10000` e `SEGURANCA_LIMPEZA_MS=60000`. Limites/capacidades precisam ser positivos, e os limiares de falha precisam ser pelo menos dois.

As sessões e tentativas continuam locais à JVM. Reiniciar encerra sessões e remove os contadores. Para várias instâncias, o limite agregado e os bloqueios precisam de armazenamento compartilhado; não foi implementado Redis nesta alteração. A retenção de eventos de auditoria no banco é uma política separada. Não há nova migração SQL nesta correção.

## Testes e evidências

| Verificação | Resultado |
| --- | --- |
| Reprodução anterior à alteração | Duas regressões HTTP falharam como esperado |
| Suíte Java | 55 passaram, sem falhas, erros ou ignorados |
| PostgreSQL 18.6 descartável | 30 passaram, incluindo limites no servidor real e regressões de rollback |
| Edge headless | 19 cenários passaram, incluindo aviso de capacidade e nova entrada após logout |
| Concorrência de sessão | 64 admissões concorrentes resultaram em exatamente 20 sessões |
| Pressão no limitador | 1.000 identidades novas não ultrapassaram a capacidade nem expulsaram bloqueios ativos |

Os testes de tempo usam um `Clock` controlado: verificam recusa até o segundo 299 e liberação no segundo 300, sem aguardar cinco minutos reais. O teste de manutenção configura um intervalo de 50 ms e observa o agendamento real removendo dados sem nova requisição. São verificados expiração por inatividade, limite absoluto, logout, descarte de usuário desativado, normalização do usuário, proteção por IP, sucesso sem zerar o IP, bloqueio mais longo que a janela e descarte em cenário de rollback.

`LimitesLoginTest` entrou na etapa PostgreSQL do CI e do script local. Os testes unitários dos mapas entram no `verify`. A suíte de navegador mantém as regressões anteriores de gravação, estoque, assinatura e sessão. O CI remoto não foi disparado nesta sessão.

Logs locais: `target/logs/limites-antes.log`, `target/logs/limites-final.log`, `target/logs/limites-postgres.log` e `target/logs/limites-e2e.log`. Relatórios Surefire separados em `target/correcao-limites/h2/` e `postgres/`. Esses arquivos são ignorados pelo Git; os testes permanentes estão em `src/test/java` e `tests/e2e`.

Somente bancos descartáveis foram utilizados. O esquema temporário PostgreSQL foi removido e o servidor de testes encerrado. O banco real do projeto e as credenciais do `.env` não foram alterados.
