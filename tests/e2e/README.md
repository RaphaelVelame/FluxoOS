# Regressões de gravação da interface

Requer JDK 25, Node 24 e o JAR compilado. Na raiz, execute `mvnw verify`; depois, nesta pasta:

```sh
npm ci
npx playwright install chromium
npm test
```

`npm test` inicia seu próprio servidor em uma porta aleatória, com H2 em memória e contas de demonstração exclusivas. Encerra o navegador e o servidor ao terminar. Não carrega `.env` nem usa o banco do projeto. O log fica em `target/e2e/server.log`.

Para usar o Edge instalado no Windows em vez de baixar Chromium, defina `$env:BROWSER_CHANNEL = 'msedge'` antes de `npm test`. Configure `JAVA_HOME` com JDK 25. O CI instala Chromium e executa estes testes depois do build Java.

São simulados erros de consulta depois de gravação confirmada, perda de resposta do POST, erros reais de gravação, envio simultâneo, consulta concorrente e expiração de sessão. As verificações conferem ordens, saldo, itens e auditoria no servidor, além do comportamento da interface. A mesma sessão de servidor existe durante a suíte, mas cada caso abre um contexto novo de navegador e cria seus próprios dados.

A suíte também verifica que a tela de login informa a capacidade de 20 sessões e permite entrar depois que um logout libera a vaga. Os testes HTTP Java usam relógio controlado para verificar o bloqueio de cinco minutos e o descarte agendado sem esperas longas.
