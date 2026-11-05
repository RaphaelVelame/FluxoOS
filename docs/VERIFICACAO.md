# Verificação da entrega

Verificado em 05/11/2026, Windows, JDK 25, com compilação direcionada ao Java 17.

- Maven Wrapper 3.9.11: `clean test` e, após a revisão final, `verify`, ambos concluídos com sucesso.
- 17 testes passaram: 12 operacionais/de segurança, um de separação dos controllers, um de proteção de dados sensíveis nos DTOs, um de carga inicial e dois de firewall HTTP.
- `StrictHttpFirewall` aplicado antes da autenticação: parâmetros de caminho com ponto e vírgula literal ou codificado retornam 400 no servidor real; página pública, login e consultas autenticadas continuam funcionando.
- Entidades e repositories JPA validados pelo Hibernate contra as migrações Flyway V1 e V2 em H2 em memória.
- Fluxo HTTP exercitado com MockMvc: login, cookie HttpOnly, CSRF, permissões, abertura, atribuição, consumo, relato e conclusão com assinatura.
- Rollback, consumo concorrente, repetição de requisições e proteção contra sobrescrita de chaves de idempotência verificados.
- Troca de senha com revogação de sessões e desativação de contas verificadas.
- Carga demonstrativa criou três ordens, três materiais, três movimentos de entrada e duas contas; uma segunda execução preservou as quantidades.
- JAR Spring Boot empacotado com Spring Data JPA/Hibernate e configuração YAML.
- Corrigida a leitura de diretórios sem destino de link no Maven Wrapper para Windows.

## Verificações pendentes

A interface estática não foi alterada nesta migração. A aparência, as interações visuais e os tamanhos de tela precisam de conferência manual no navegador; os testes HTTP não substituem essa validação.

Não havia instância PostgreSQL/Docker disponível para teste. O perfil PostgreSQL está em `application-postgres.yml`; os testes transacionais executados usam H2 em modo PostgreSQL.

Para conferir manualmente: execute `scripts/executar.ps1 -Demo` numa base nova, entre com `admin`, abra uma OS, designe `tecnico`, inicie, registre material, salve relato e conclua com assinatura. Confira também a navegação em 360px de largura, o formulário de assinatura por toque e as permissões ao entrar como técnico.
