# Regressões de gravação da interface

Requer JDK 25, Node 24 e o JAR compilado. Na raiz, execute `mvnw verify`; depois, nesta pasta:

```sh
npm ci
npx playwright install chromium
npm test
```

`npm test` executa duas suítes com servidores próprios em portas aleatórias e H2 em memória.
Uma usa contas de demonstração; a outra provisiona um master e duas empresas somente para o
ensaio. Os navegadores e servidores são encerrados ao terminar. Não carrega `.env` nem usa o
banco do projeto. Os logs ficam em `target/e2e/server.log` e `target/e2e/master-server.log`.

Para usar o Edge instalado no Windows em vez de baixar Chromium, defina `$env:BROWSER_CHANNEL = 'msedge'` antes de `npm test`. Configure `JAVA_HOME` com JDK 25. O CI instala Chromium e executa estes testes depois do build Java.

São simulados erros de consulta depois de gravação confirmada, perda de resposta do POST, erros reais de gravação, envio simultâneo, consulta concorrente e expiração de sessão. As verificações conferem ordens, saldo, itens e auditoria no servidor, além do comportamento da interface. A mesma sessão de servidor existe durante a suíte, mas cada caso abre um contexto novo de navegador e cria seus próprios dados.

A suíte também verifica que a tela de login informa a capacidade de 20 sessões e permite entrar depois que um logout libera a vaga. Os testes HTTP Java usam relógio controlado para verificar o bloqueio de cinco minutos e o descarte agendado sem esperas longas.

As regressões de sessão retêm respostas até depois de logout e novo login. Um 401 antigo não
pode encerrar a sessão nova, e os detalhes de uma OS da conta anterior não podem reaparecer
após entrar em outra empresa. O ensaio master verifica entrada, atualização, recarga da página,
consulta global e ausência de chamadas às rotas de estoque que exigem tenant operacional.

A gestão visual master também é exercitada com cadastros sintéticos: empresa, administrador,
primeiro login, seleção de empresa para funcionários, edição, revogação de sessão, status,
proteção do último administrador e recuperação de consultas após gravações confirmadas.
Cancelar ou concluir um formulário deve remover seus campos e credenciais da página.
