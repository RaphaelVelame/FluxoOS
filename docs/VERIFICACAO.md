# Verificação da entrega

Verificado em 11/11/2026, Windows, Temurin JDK 25.0.5, com compilação direcionada ao Java 25.

## Resultados atuais

- Maven Wrapper 3.9.11: `verify` concluído com sucesso e JAR Spring Boot empacotado.
- Suíte Java: 55 testes passaram, sem falhas, erros ou testes ignorados.
- PostgreSQL 18.6: 30 testes de integração passaram em esquemas temporários isolados, removidos ao final.
- Navegador Edge sem interface: 19 cenários automatizados passaram, sem falhas, cancelamentos ou testes ignorados.
- `git diff --check` e análise de sintaxe dos scripts PowerShell de execução e testes PostgreSQL concluídos sem erros.

## Comportamentos verificados

- Autenticação, cookie HttpOnly, CSRF, permissões e firewall HTTP, incluindo proteção das rotas sob contexto `/fluxo`.
- Revogação de sessões por desativação e troca de senha; reativar uma conta não recupera sessões antigas.
- Até 20 sessões simultâneas por instância, recusa da 21ª e liberação de capacidade após logout ou descarte de sessões inválidas.
- Bloqueio de login por cinco minutos após cinco erros por usuário, limite adicional por IP e limpeza periódica dos controles expirados.
- Abertura, atribuição, consumo de material, relato e conclusão de OS; rollback e consumo concorrente.
- Idempotência da abertura de OS: repetição da mesma chave e conteúdo não duplica a ordem; conteúdo diferente com a mesma chave é recusado.
- Confirmação da gravação preservada quando a consulta seguinte falha; recuperação da tela sem reenviar a operação.
- Migrações Flyway V1 a V4 e preservação dos dados existentes ao introduzir a versão de sessão.

O CI está configurado para executar a suíte Java com JDK 25, os cenários de navegador com Chromium e os 30 testes de integração em PostgreSQL 18 descartável. A execução remota deve ser conferida no GitHub Actions após o envio; os resultados acima são locais.

As evidências temporárias ficam em `target/logs/` e nos relatórios de teste sob `target/`, ignorados pelo Git e sujeitos a limpeza. Os testes reproduzíveis permanecem em `src/test/java` e `tests/e2e`. Os relatórios de auditoria e correção nesta pasta detalham os resultados e as limitações de cada etapa.

## Verificações pendentes

Backup/restauração e carga prolongada ainda precisam de ensaio no ambiente de implantação. A aparência em telas pequenas e a assinatura por toque precisam de conferência manual. Os testes automatizados não representam certificação de que a aplicação está pronta para qualquer ambiente de produção.

Os limites de sessões e tentativas são mantidos na memória de cada instância. Uma implantação com múltiplas instâncias exige armazenamento compartilhado para aplicar limites globais. O contexto `/fluxo` foi validado para a proteção HTTP; os caminhos absolutos da interface ainda exigem ajuste para publicar toda a aplicação nesse contexto.

Para conferir manualmente: execute `scripts/executar.ps1 -Demo` numa base nova, entre com `admin`, abra uma OS, designe `tecnico`, inicie, registre material, salve relato e conclua com assinatura. Confira também a navegação em 360px de largura e as permissões ao entrar como técnico.
