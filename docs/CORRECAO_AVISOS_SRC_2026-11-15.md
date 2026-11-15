# Marcações de problemas no src

O aviso do explorador “contém itens enfatizados” indicava diagnósticos dos arquivos dentro de
`src`. O registro de problemas do Java no VS Code identificava três avisos e dois erros:

| Arquivo | Diagnóstico | Correção |
| --- | --- | --- |
| `OperacaoController.java` | Campo `usuarios` sem uso | Removidos o campo e a dependência do construtor |
| `ControleTentativas.java` | Import de classe do próprio pacote sem uso | Removido o import redundante |
| `UsuarioService.java` | Import `Contratos.UsuarioView` sem uso | Removido o import |
| `FluxoOperacionalTest.java` | `List.getFirst()` não reconhecido | Corrigida a configuração local do Java no editor |
| `MultiTenantRbacTest.java` | `List.getFirst()` não reconhecido | Corrigida a configuração local do Java no editor |

O `pom.xml` já exigia Java 25, mas o modelo Maven mantido pelo editor ainda referenciava
`JavaSE-17` no classpath e nível de compilação 17 nas preferências do projeto. `List.getFirst()`
é válido no Java 25 usado para compilar e executar o aplicativo.

Foi ajustado o `.vscode/settings.json` local para selecionar o JDK 25 disponível em `.tools` e
atualizar automaticamente a configuração de compilação. As demais preferências foram preservadas.
O arquivo local permanece ignorado pelo Git, pois contém o caminho do JDK desta máquina.
O `pom.xml` recebeu uma notificação de atualização para o editor reimportar seu modelo.

Em outro computador, selecione um JDK 25 na configuração Java do editor. Se o VS Code já estava
aberto e ainda mostrar os diagnósticos antigos, execute **Java: Update Project Configuration**
na paleta de comandos. Para um índice antigo que persista, use **Java: Clean Java Language Server
Workspace** e deixe a extensão reiniciar e importar o projeto novamente.

Validação realizada:

- Maven `verify` com Java 25: 108 testes, zero falhas e zero erros.
- Compilação de todos os arquivos Java com o ECJ da extensão Java instalada, Java 25 e as
  preferências já existentes do editor: zero erros e zero avisos. Imports e membros privados
  sem uso foram tratados como erro nessa verificação.
- Verificações do aplicativo local com administrador da empresa e master aprovadas durante o
  diagnóstico. A aplicação continuou respondendo na porta 8080.

A atualização visual dos diagnósticos depende de o editor recarregar o modelo do projeto.
