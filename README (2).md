# FluxoOS — Sistema de Ordens de Serviço e Controle de Estoque

> Projeto em desenvolvimento | Java 17+ | Orientação a Objetos | Segurança | OS + estoque integrado

## Sobre o projeto

O **FluxoOS** é um aplicativo pensado para gerenciar **ordens de serviço (OS)** e **materiais em estoque** no mesmo fluxo de trabalho. Quando um técnico autorizado registra o uso de um material em uma OS, o sistema deve **baixar automaticamente a quantidade correspondente no estoque** e registrar a movimentação para consulta e auditoria.

O objetivo é facilitar o trabalho de gestores, equipes de campo e suporte de TI, com informações organizadas, histórico das operações e regras claras de permissão. O produto final deverá ser acessível em **computadores e celulares**.

**Estado atual:** existe uma implementação didática do domínio e de componentes de segurança em **Java 17+, executada em memória**, com exemplos e verificações automatizadas. Essa implementação **não é ainda uma aplicação web ou móvel pronta para uso**. API, interface, banco persistente e implantação fazem parte das próximas etapas.

## Objetivos

- Centralizar a abertura, o acompanhamento e a conclusão das ordens de serviço.
- Permitir que a equipe descreva o atendimento realizado no local.
- Integrar o consumo registrado na OS à baixa automática do estoque.
- Permitir que todas as contas consultem o estoque, com alterações restritas.
- Exigir a assinatura da pessoa responsável presente no local para concluir a OS.
- Preservar rastreabilidade de atribuições, movimentos e eventos de segurança.
- Evoluir para uso seguro em dispositivos móveis e computadores.

## Perfis e permissões

| Ação | Chefe | Suporte de TI | Técnico / equipe |
| --- | :---: | :---: | :---: |
| Visualizar estoque | ✅ | ✅ | ✅ |
| Cadastrar, repor ou ajustar estoque manualmente | ✅ | ✅ | ❌ |
| Registrar uso de material na OS e gerar baixa automática | Conforme atribuição e regra da OS | Conforme atribuição e regra da OS | ✅, quando designado |
| Abrir OS com problema, solicitante e local | ✅ | Conforme autorização futura | Conforme autorização futura |
| Atribuir equipe ou técnico | ✅ | Conforme regra administrativa | ❌ |
| Registrar descrição do serviço executado | Conforme atribuição | Conforme atribuição | ✅, quando designado |
| Concluir OS sem assinatura do responsável | ❌ | ❌ | ❌ |

**Regra importante:** a baixa por **consumo em OS** não significa que o técnico tem permissão para **alterar o estoque manualmente**. O consumo é uma operação de negócio controlada, vinculada à OS e ao técnico designado. As permissões deverão ser verificadas também no backend de cada operação, não apenas na interface.

## Fluxo de uma ordem de serviço

1. **Abertura:** o chefe informa o problema relatado, quem abriu o chamado e o local onde a equipe deve comparecer.
2. **Atribuição:** o responsável designa o técnico ou a equipe e o sistema registra o histórico da atribuição.
3. **Atendimento:** o técnico designado descreve o que foi feito e, se necessário, informa os materiais utilizados.
4. **Baixa de estoque:** a aplicação valida a operação, a quantidade e o saldo; registra o item na OS e a movimentação de saída correspondente.
5. **Assinatura:** a pessoa responsável presente no local assina o atendimento.
6. **Conclusão:** a OS só pode ser concluída após cumprir as validações obrigatórias, incluindo a referência de assinatura.

### Estados da OS

Na implementação de domínio atual, o enum `Situacao` define:

- `ABERTA`
- `EM_ANDAMENTO`
- `CONCLUIDA`

As transições são controladas por operações da ordem de serviço, evitando mudanças arbitrárias de estado.

### Exemplo de baixa automática

Se o estoque tem **20 conectores RJ45** e um técnico designado registra o uso de **2 conectores** em uma OS, o resultado esperado é:

- Estoque restante: **18 conectores**.
- Item adicionado à OS: **2 conectores RJ45**.
- Histórico de estoque: **saída de 2 conectores, vinculada à OS, ao usuário e à data**.

Em produção, essas alterações devem fazer parte de **uma transação atômica**: ou todas são confirmadas, ou nenhuma é gravada. Também será necessário tratar concorrência e evitar baixas duplicadas por reenvio da mesma operação.

## Tecnologias

### Utilizadas na versão Java didática

- **Java 17+** — implementação das entidades, regras do domínio e componentes de segurança.
- **POO** — encapsulamento, interfaces, herança, polimorfismo e composição.
- **Repositórios em memória** — simulação de armazenamento para testes e demonstrações.
- **Compilador `javac`** — execução local sem dependências externas obrigatórias na versão atual.

### Planejadas / recomendadas para a aplicação final

| Camada | Tecnologia proposta | Finalidade |
| --- | --- | --- |
| Backend / API | Java + Spring Boot | Disponibilizar operações REST |
| Autorização e infraestrutura de autenticação web | Spring Security | Proteger endpoints e integrar identidade e permissões |
| Persistência | PostgreSQL | Gravar usuários, OS, materiais e histórico |
| Acesso a dados | JPA/Hibernate ou JDBC | Integrar o domínio ao banco, com mapeamento apropriado |
| Migrações | Flyway | Versionar alterações do esquema do banco |
| Testes | JUnit + Mockito + testes de integração | Verificar regras, permissões e transações |
| Interface para computador e celular | A definir | Consumir a API com layout responsivo ou cliente multiplataforma |

> Essas tecnologias de produção são **propostas de evolução**, não dependências já integradas ao código Java atual.

## Organização do projeto atual

```text
FluxoOS/
├── src/
│   ├── entities/
│   │   ├── Usuario.java
│   │   ├── Perfil.java
│   │   ├── OrdemServico.java
│   │   ├── ItemOS.java
│   │   ├── Material.java
│   │   ├── MovimentoEstoque.java
│   │   ├── EstoqueService.java
│   │   ├── EventoAuditoria.java
│   │   ├── EventoAtribuicao.java
│   │   ├── RegistroBase.java
│   │   ├── Auditavel.java
│   │   ├── Validacoes.java
│   │   └── ...
│   ├── seguranca/
│   │   ├── Sessao.java
│   │   ├── AutenticacaoService.java
│   │   ├── SessaoService.java
│   │   ├── TrocaSenhaService.java
│   │   ├── LimitadorTentativas.java
│   │   ├── HasherSenha.java
│   │   ├── RepositorioUsuarios.java
│   │   ├── RepositorioSessoes.java
│   │   ├── EventoSeguranca.java
│   │   └── ...
│   └── exemplo/
│       ├── Demo.java
│       ├── TesteDominio.java
│       ├── DemoSeguranca.java
│       └── TesteSeguranca.java
├── docs/
│   ├── MUDANCAS.md
│   ├── MUDANCAS_SEGURANCA.md
│   ├── fluxoos.puml
│   └── fluxoos-seguranca.puml
├── scripts/
│   ├── testar.ps1
│   └── testar.sh
└── README.md
```

Essa árvore representa a organização da versão didática com segurança anteriormente elaborada. Dependendo da versão do código que estiver aberta no VS Code, alguns nomes e arquivos poderão variar.

### Responsabilidade dos pacotes

- **`entities`**: domínio de negócio (usuário, ordem de serviço, material, movimentações e regras locais). O `EstoqueService` ainda está aqui por conveniência didática.
- **`seguranca`**: sessões, autenticação, troca de senha, limitação de tentativas, eventos e contratos de repositório.
- **`exemplo`**: demonstrações executáveis e verificações do comportamento.
- **`docs`**: explicações técnicas e diagramas UML.

**Próxima refatoração arquitetural:** separar `domain`, `application`, `infrastructure`, `api` e `security`/`seguranca`, preservando o comportamento e evitando dependências indevidas entre camadas. A reorganização dos pacotes ainda **não foi concluída**.

## Conceitos de orientação a objetos aplicados

- **Encapsulamento:** as entidades protegem seu estado; alterações importantes passam por métodos com validações.
- **Interfaces:** `Auditavel`, `HasherSenha` e repositórios representam contratos, permitindo substituir implementações.
- **Herança:** `ControleTentativas` compartilha comportamento com `TentativaLogin` e `TentativaLoginIP`.
- **Polimorfismo:** serviços operam com interfaces de repositório, sem depender de uma implementação específica em memória.
- **Imutabilidade e rastreabilidade:** itens e eventos importantes não devem ser livremente alterados após registrados.

## Segurança

O módulo didático de segurança já explora:

- Hash de senhas com **PBKDF2-HMAC-SHA256** e salt individual.
- Tokens de sessão aleatórios, com armazenamento de seus hashes.
- Expiração e revogação de sessões.
- Verificação de token CSRF nas operações previstas pelo módulo.
- Controle de tentativas de login por usuário e por IP.
- Registro de eventos de segurança, sem armazenar senhas ou tokens em logs.
- Uso de contratos de repositório e dependências injetadas para facilitar testes.

**Limitações atuais:** os dados são armazenados em memória e não há API HTTP, banco permanente, cookies de produção, autenticação multifator nem controle distribuído de concorrência e limites de requisições. O código é um ponto de partida para aprendizagem, **não uma solução segura para implantação real sem novas etapas de engenharia, testes e revisão**.

Para produção, deverão ser implementados e revisados: HTTPS, proteção dos endpoints, sessões/cookies seguros, autorização no servidor, persistência, transações, controle de concorrência, prevenção de requisições duplicadas, auditoria durável, armazenamento protegido das assinaturas, backups e gestão de segredos.

## Como executar no VS Code

**Pré-requisitos:** JDK 17 ou superior, Visual Studio Code e a extensão **Extension Pack for Java**.

1. Abra a pasta do projeto no VS Code.
2. Acesse **Terminal > Novo Terminal**.
3. Na raiz do projeto, execute o script adequado ao seu sistema operacional, caso os arquivos `scripts/` estejam presentes.

**Windows (PowerShell):**

```powershell
./scripts/testar.ps1
```

**Linux / macOS:**

```bash
./scripts/testar.sh
```

**Alternativa no Windows: compilar manualmente sem executar scripts:**

```powershell
New-Item -ItemType Directory -Force build | Out-Null
$arquivos = @(Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName })
javac -encoding UTF-8 -d build $arquivos
java -cp build exemplo.TesteDominio
java -cp build exemplo.TesteSeguranca
java -cp build exemplo.DemoSeguranca
```

Na versão de referência, as classes de teste demonstram verificações do domínio e da segurança. **Não há ainda um endereço web ou tela de login para abrir no navegador** por meio dessa execução Java.

## Roadmap

- [x] Definir os fluxos principais de OS e estoque.
- [x] Modelar classes de domínio, enums e interfaces em Java.
- [x] Implementar exemplo em memória da baixa vinculada à OS.
- [x] Modelar atribuição, estados da OS e exigência de referência de assinatura.
- [x] Criar componentes didáticos de autenticação, sessão e auditoria.
- [x] Criar diagramas UML e exemplos de execução/testes locais.
- [ ] Reorganizar o projeto em camadas de domínio, aplicação, infraestrutura e API.
- [ ] Criar API REST com autenticação/autorização de endpoints.
- [ ] Criar banco de dados permanente e migrações.
- [ ] Implementar transações de estoque com concorrência e idempotência.
- [ ] Criar interface responsiva para computadores e celulares.
- [ ] Implementar captura e armazenamento seguro da assinatura no local.
- [ ] Implementar testes de integração, controle de acesso e segurança.
- [ ] Preparar implantação, observabilidade, backups e recuperação.

## Documentação complementar

Na distribuição do projeto que inclui a pasta `docs/`, consulte:

- `docs/MUDANCAS.md`: evolução e justificativas das classes do domínio.
- `docs/MUDANCAS_SEGURANCA.md`: classes e decisões do módulo de segurança.
- `docs/fluxoos.puml`: diagrama de classes do domínio.
- `docs/fluxoos-seguranca.puml`: diagrama do módulo de segurança.

## Contribuição e licença

Projeto em desenvolvimento e estudo. Antes de receber contribuições externas ou ser publicado como software reutilizável, devem ser definidos o processo de contribuição e a **licença**. A ausência de um arquivo de licença não significa que o projeto seja de código aberto.

---

**FluxoOS** — Ordem de serviço, equipe e estoque conectados em um único processo.
