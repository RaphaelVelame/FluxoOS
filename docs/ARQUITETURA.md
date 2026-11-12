# Arquitetura e decisões

O projeto usa uma estrutura direta por responsabilidade, com controllers REST, services, repositories Spring Data JPA, entidades, DTOs e configuração. Enums e componentes de segurança têm seus próprios pacotes.

```mermaid
flowchart LR
    UI[Interface mobile first] --> HTTP[Controladores HTTP]
    HTTP --> DTO[DTOs de entrada e saída]
    HTTP --> SERVICE[Services e transações]
    SERVICE --> ENTITY[Entidades JPA]
    SERVICE --> REPO[Repositories Spring Data JPA]
    REPO --> SQL[(H2 / PostgreSQL)]
    CONFIG[Configuração YAML e Spring] --> SERVICE
```

`controller` recebe e valida requisições. `service` contém as regras de estoque, OS, contas e autenticação. `repository` contém as interfaces `JpaRepository` e o armazenamento de sessões em memória. `entity` mapeia usuários, materiais, ordens, itens, movimentos, auditoria e idempotência. `dto` define os contratos HTTP. `config` reúne o filtro HTTP, beans de configuração e carga inicial. `enums` reúne todos os enums, inclusive os de segurança; `seguranca` contém PBKDF2, tokens e controle de tentativas.

As dependências são injetadas por construtor. Os services delimitam as transações com `@Transactional`, e o Hibernate acompanha as alterações nas entidades carregadas. As leituras são convertidas em DTOs dentro das transações, com `open-in-view: false`. Os controllers não acessam repositories nem abrem transações. O backend fornece JSON e a interface estática consome a API; não há portas, adaptadores ou templates MVC.

`application.yml` configura o H2 por padrão e `application-postgres.yml` configura o perfil PostgreSQL. No Windows, `scripts/executar.ps1` carrega a conexão e o perfil do `.env` local, verifica a autenticação com `psql` e só solicita a senha inicial quando o banco selecionado não tem usuários. O Flyway 11.20.0 aplica o esquema, incluindo suporte ao PostgreSQL 18, e o Hibernate usa `ddl-auto: validate`. A sequência existente continua gerando os IDs com alocação de um por vez. A V1 permanece intacta e a V2 acrescenta `alvo_id` à auditoria, preservando os dados existentes.

## Estoque consistente

O consumo bloqueia a OS e depois o material usando `@Lock(PESSIMISTIC_WRITE)` nos repositories JPA. Após validar atribuição, estado, duplicidade, material ativo, quantidade e saldo, grava saldo, item, movimento, auditoria e idempotência. Qualquer falha reverte a transação inteira. Os itens são uma coleção JPA da OS, persistida na tabela `itens_os`.

A chave `Idempotency-Key` é obrigatória no consumo e na movimentação manual. Ela é vinculada ao usuário e ao hash do conteúdo. Reenvios idênticos devolvem o mesmo ID; conteúdo diferente com a mesma chave é recusado. O frontend mantém a chave enquanto o formulário está aberto. Após uma perda de resposta, repita sem fechar o formulário; recarregar a página cria uma operação nova, e o histórico deve ser consultado antes de repetir reposições manuais.

A chave primária de `itens_os` impede material duplicado na mesma OS. O projeto original já adotava esse comportamento: a aplicação não inventa edição silenciosa do consumo.

As leituras de listagem carregam os dados existentes sem paginação; essa escolha atende à base local inicial. Bases maiores deverão usar paginação no servidor, consultas resumidas, índices medidos e reduzir o carregamento de agregados/itens por linha.

## Autenticação

Os componentes PBKDF2, sessão opaca, CSRF, bloqueio e troca de senha foram preservados. Usuários, credenciais e eventos são persistidos por JPA. A troca de senha usa atualização condicional para detectar alterações concorrentes. Falhas de login são auditadas antes de recusar a autenticação. Sessões ficam em memória, com limite absoluto de oito horas e inatividade de trinta minutos. Os tokens secretos só são emitidos no login. O token de sessão fica em cookie HttpOnly; CSRF fica em sessionStorage por aba. Uma nova aba pode exigir novo login para obter CSRF.

O sistema serve UI e API na mesma origem. Requisições mutáveis exigem sessão e CSRF; quando presente, o cabeçalho Origin deve corresponder à origem do servidor. O IP vem da conexão, sem confiar em X-Forwarded-For arbitrário. Atrás de proxy, configure origem e repasse confiável antes de abrir acesso externo. Não há CORS amplo.

As assinaturas PNG são validadas por formato, dimensões e presença de traços, armazenadas na mesma transação da conclusão e servidas apenas em rota autenticada. A listagem retorna um indicador de assinatura. Banco, backups e acesso de consulta precisam ser tratados segundo a política da organização.

### Invalidação de sessões por conta

A migração V3 acrescenta `usuarios.versao_sessao`, inicialmente zero para as contas existentes. Cada sessão guarda a versão observada durante a autenticação. Desativar incrementa essa versão na mesma transação que muda o status; reativar mantém a versão. A troca de senha também a incrementa na atualização condicional do hash. `SessaoService` compara a versão em cada validação, além de verificar conta ativa, expiração e CSRF.

Assim, sessões anteriores continuam inválidas após a reativação, inclusive se a emissão terminar com um estado da conta carregado antes da desativação. Rollback preserva a versão anterior e suas sessões. Não há necessidade de percorrer os mapas de todas as instâncias para invalidar a conta. As sessões e o limitador continuam em memória por JVM, com os limites e a manutenção descritos abaixo. Consulte [pesquisa e testes da correção](CORRECAO_SESSOES_2026-11-11.md).

### Capacidade e descarte da autenticação

O armazenamento admite no máximo 20 sessões por JVM, com verificação de capacidade e inclusão sincronizadas. Expiradas e revogadas são removidas durante consultas/admissão, e logout remove a sessão imediatamente. Antes de emitir um token, `SessaoService` também descarta sessões cujo usuário foi removido, desativado ou teve sua versão alterada. A consulta resumida dos usuários usa uma transação independente de leitura para observar somente commits, evitando descarte indevido durante rollback. O mapa não fica bloqueado durante o acesso ao banco.

O limitador registra apenas falhas, limita os mapas de nomes e IPs e não aloca entradas em consultas de identidades novas. Descarte respeita a janela de falhas e todo o prazo de bloqueio. Na saturação, não substitui bloqueios ativos por identidades novas. A verificação e o registro de falhas continuam coordenados pelo autenticador sincronizado. `ManutencaoSeguranca` executa a limpeza periódica, por padrão a cada 60 segundos, mesmo sem tráfego. Consulte [política, pesquisa e testes](CORRECAO_LIMITES_2026-11-11.md).

## Interface

HTML semântico, CSS mobile first e módulos JavaScript nativos. A navegação inferior é a base para celular; a sidebar aparece a partir de 900px. Formulários têm rótulos, foco visível, feedback e botões desabilitados durante envio. Dados de usuário são escapados antes de montar HTML. Scripts e estilos são locais, sem CDN ou fontes externas.

### Confirmação de gravação e recuperação da tela

Os modais executam somente a gravação em `aoEnviar`. Depois do sucesso, bloqueiam novo envio, fecham e executam `aoSalvar`; esse retorno confirmado não é tratado como falha de gravação quando as consultas posteriores falham. `aposGravar` mantém uma confirmação pendente e mostra uma página de recuperação, sem ações baseadas em dados antigos. O botão Atualizar dados executa somente consultas e restaura a rota atual. Início e relato usam a mesma separação. Se a sessão expirar nesse intervalo, a tela de login preserva a mensagem de confirmação.

A migração V4 acrescenta autor, chave e hash da abertura em `ordens`, com unicidade por autor/chave. O servidor compara um hash dos campos da solicitação e serializa aberturas do mesmo autor com lock transacional no usuário. Repetições retornam o DTO da mesma OS dentro da transação; dados diferentes com a mesma chave são recusados. Conta, ordem e auditoria mantêm as transações existentes. Ordens anteriores à migração continuam válidas, sem chave de abertura.

Os testes de navegador em `tests/e2e` usam Playwright e Node somente como ferramentas de desenvolvimento. A interface continua em JavaScript nativo, sem framework adicional. Consulte [pesquisa e validação da correção](CORRECAO_GRAVACAO_2026-11-11.md).

## Evolução recomendada

Os testes operacionais executam em PostgreSQL 18 no CI e podem ser repetidos localmente com `scripts/testar-postgres.ps1`, em um esquema temporário isolado. Antes de escalar, adicionar sessões e rate limit compartilhados, paginação, recuperação de acesso, políticas de retenção, transações para correção/devolução vinculada à OS e operações de backup/restauração ensaiadas.
