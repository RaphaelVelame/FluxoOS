# Gestão visual do master — 15/11/2026

A conta master pode cadastrar empresas e seus administradores pelo navegador e gerenciar
os usuários por empresa. São usadas as APIs existentes, sem novas migrations. Permissões,
empresa imutável do usuário e proteção do último administrador continuam no backend.

## Como usar

1. Entre como master e abra **Visão global**.
2. Clique em **Cadastrar empresa**, informe o nome e confirme.
3. No cartão da empresa, clique em **Adicionar administrador**.
4. Confira a empresa e informe nome, login único na plataforma e senha inicial com pelo
   menos 15 caracteres. A função Administrador da empresa é obrigatória nesse formulário;
   funções operacionais adicionais são opcionais.
5. Após salvar, a tela abre os usuários da empresa. A nova conta já pode entrar para
   gerenciar sua equipe, ordens e materiais.

Empresa e administrador são cadastrados em operações separadas. A empresa sem administrador
ativo fica identificada no painel, permitindo concluir seu acesso depois. Se uma gravação
for confirmada e a consulta seguinte falhar, **Atualizar dados** recupera a tela sem repetir
o cadastro.

**Gerenciar usuários** abre a lista já filtrada. O master pode alternar a empresa, buscar
nome/login, criar contas, editar nome/funções e ativar/desativar acessos com confirmação.
A empresa da conta não é editável. Alterar funções invalida as sessões do destinatário.
O último administrador ativo não pode ser desativado nem perder essa função: a tela orienta
cadastrar outro, e o servidor mantém a proteção mesmo em chamadas diretas.

O administrador de empresa continua limitado à própria empresa e às quatro funções
operacionais. O cadastro ou edição de contas master não é oferecido nesses formulários.
O estoque operacional continua sendo acessado pelas contas de empresa.

## Implementação e verificação

- Nova view `platform.js` para empresas e acesso a seus administradores.
- Seleção de empresa, filtros, gestão de funções e status em `team.js`.
- Auditoria `EMPRESA_CRIADA` na mesma transação do cadastro; rollback reverte ambos.
- Formulários removidos ao fechar, cancelar, confirmar ou sair da conta. Credenciais não
  permanecem em campos ocultos. Erros de validação preservam os dados para correção.
- Nenhuma mudança de schema ou redefinição de contas existentes.

| Verificação | Resultado |
| --- | --- |
| Maven verify, Java 25/H2 | 98 testes aprovados |
| PostgreSQL 18 descartável | 67 testes aprovados |
| Playwright/Edge, servidores H2 exclusivos | 33 testes aprovados |
| Importação sintética | Preservação, repetição segura e rollback aprovados |
| JavaScript | 12 arquivos com sintaxe verificada |

Os ensaios cobrem cadastro visual, login duplicado com correção, vínculo da conta à empresa,
primeiro acesso do administrador, isolamento, edição com revogação de sessão, status,
último administrador ativo, cancelamento, múltiplas funções e falha de consulta após gravação
confirmada. Usam apenas cadastros sintéticos. Capturas de celular ficam em
`target/e2e/master-empresas-mobile.png` e `target/e2e/master-usuarios-mobile.png`.

```powershell
.\mvnw.cmd -Dfluxoos.build.directory=target/gestao-master-entrega verify
.\scripts\testar-postgres.ps1 -IncluirImportacao
$env:FLUXOOS_TEST_JAR = (Resolve-Path target/gestao-master-entrega/fluxoos-1.0.0-SNAPSHOT.jar).Path
$env:BROWSER_CHANNEL = 'msedge'
npm test --prefix tests/e2e
```

A atualização local usa o JAR de `target/gestao-master-entrega`, a conexão PostgreSQL já
configurada e nenhuma carga demonstrativa. A verificação na base local usa login e consultas;
não cadastra empresas, usuários ou OS de exemplo nos dados reais.
