# Correção: gravação confirmada e atualização da tela

Implementada em 11/11/2026. O objetivo é preservar o resultado confirmado de uma gravação e permitir recuperar as consultas sem repetir a operação.

## Pesquisa e decisão

Foram consultados o [guia de invalidação após mutações no GitHub do TanStack Query](https://github.com/TanStack/query/blob/main/docs/framework/react/guides/invalidations-from-mutations.md), a [discussão sobre sucesso da mutação no Stack Overflow](https://stackoverflow.com/questions/76754496/how-to-resolve-if-a-react-query-mutation-was-success-or-not), o [artigo técnico da Stripe](https://stripe.com/blog/idempotency) e sua [documentação de requisições idempotentes](https://docs.stripe.com/api/idempotent_requests). A implementação se apoia nas fontes primárias e nas regressões locais; a discussão foi usada para comparar os ciclos de gravação e consulta.

A solução combina duas garantias distintas:

- Separar o sucesso do comando da recuperação dos dados. Um GET posterior com erro não altera o fato de que o POST foi confirmado.
- Identificar a abertura de OS no servidor. Quando a resposta do POST se perde, repetir a mesma chave e os mesmos dados recupera a mesma ordem.

Não foi adicionado React nem TanStack Query à aplicação. O padrão foi implementado nos módulos JavaScript já existentes. Apenas esconder o erro de consulta deixaria a tela com ações desatualizadas; apenas adicionar idempotência ainda mostraria a gravação confirmada como falha. Por isso foram corrigidos os dois lados.

## Comportamento da interface

`modal` agora aceita `aoEnviar`, responsável apenas pela gravação, e `aoSalvar`, executado depois da confirmação. O modal fecha e não aceita novo envio após sucesso. Envios simultâneos são bloqueados; durante uma requisição em andamento, fechar/cancelar o modal não descarta a tentativa.

Se uma gravação for recusada, o formulário permanece aberto com os valores e o erro, permitindo correção. Se a gravação for confirmada e as consultas falharem, a página mostra a confirmação e um botão Atualizar dados. Esse botão executa somente GETs. Enquanto a confirmação estiver pendente, a navegação não reapresenta ações baseadas no estado anterior.

O fluxo foi aplicado a abertura, atribuição, início, relato, consumo e conclusão com assinatura de OS; cadastro, movimentação e status de material; cadastro e status de conta. O login confirmado também pode recuperar suas consultas sem repetir credenciais. Uma expiração durante a consulta posterior preserva o aviso da operação concluída na tela de login. Respostas de consultas pertencentes a uma sessão encerrada são descartadas.

## Idempotência da abertura de OS

`POST /api/ordens` agora exige `Idempotency-Key`, seguindo a regra de 16 a 80 letras, números ou hífens já usada no estoque. O formulário cria uma chave por abertura e a conserva nas retentativas enquanto estiver aberto.

A migração aditiva V4 acrescenta três campos a `ordens`: autor, chave e hash do conteúdo, mais uma restrição de unicidade. O hash inclui os campos com seus comprimentos em bytes para evitar ambiguidades na concatenação. O lock do usuário serializa aberturas daquele autor até o commit; a unicidade do banco protege a identidade da operação. Rollback não reserva a chave e não deixa ordem/auditoria parcial.

Mesma chave, mesmo autor e mesmos dados retornam a mesma OS; dados diferentes retornam 409. O DTO é montado dentro da transação para permitir repetir uma abertura já persistida, sem erro de carregamento de coleções. A resposta representa o estado atual da ordem, não uma cópia histórica do primeiro JSON.

Cada nova operação intencional usa outra chave. Fechar um formulário após um resultado desconhecido e iniciar uma nova abertura, ou recarregar a página, cria outra tentativa: consulte as ordens antes de iniciar outra operação. A chave não é persistida no navegador entre recargas. A proteção de resposta perdida foi acrescentada à abertura; os demais endpoints mantêm seus próprios contratos, inclusive a idempotência já existente no estoque.

## Validação e entrega

Antes da alteração, dois testes de navegador falharam: o modal continuava aberto após um POST confirmado seguido de erro de consulta; a perda da resposta do POST permitia criar duas ordens.

- Build e suíte Java: 38 testes passaram, sem falhas, erros ou ignorados.
- PostgreSQL 18.6 descartável: 25 testes passaram, incluindo concorrência de abertura, repetição, conflito de conteúdo, rollback, separação de autores e atualização de banco V2 para V4 preservando dados existentes.
- Navegador: 18 cenários passaram, sem falhas, em `tests/e2e/gravacao.test.mjs`, com falhas de consulta em todos os formulários afetados, perda de resposta, erro real de gravação, envio simultâneo, consulta concorrente, login e expiração de sessão. O resultado está no log `target/logs/gravacao-e2e-final.log`.
- CI: agora instala as dependências de teste da interface, instala Chromium e executa a suíte após o build. O job remoto não foi disparado nesta sessão; a validação local usou Edge headless.

O banco real do projeto não foi alterado nos testes. Flyway aplicará V4 ao iniciar o JAR atualizado no banco selecionado. Clientes que abrem OS pela API devem enviar a nova chave obrigatória. Não há mudança de formato nos DTOs de resposta.

Evidências locais: `target/logs/gravacao-e2e-antes.log`, `target/logs/gravacao-final.log`, `target/logs/gravacao-postgres.log`, `target/logs/gravacao-e2e-final.log` e `target/correcao-gravacao/`. São ignoradas pelo Git e podem ser removidas por limpezas futuras. Os testes de regressão permanecem no código e no CI.
