# Verificação da entrega

Verificado em 03/11/2026, Windows, JDK 25, com compilação direcionada ao Java 17.

- 13 testes automatizados passaram: 11 testes operacionais/de segurança, um de arquitetura e um de proteção de dados sensíveis nos contratos de log.
- Maven Wrapper 3.9.11 executado com validação SHA-256 da distribuição oficial.
- JAR Spring Boot empacotado com sucesso.
- Oito módulos JavaScript analisados pelo parser de módulos do Node, sem erro de sintaxe.
- Aplicação iniciada a partir do JAR, com migração Flyway e H2 em arquivo numa base isolada em `target/qa`.
- Página inicial, JavaScript e CSS responderam HTTP 200. Login, cookie HttpOnly, consultas autenticadas e logout foram exercitados no servidor real.
- A carga demonstrativa produziu três ordens, três materiais e duas contas.
- O código de estilos foi expandido em blocos legíveis, preservando seu conteúdo.

## Verificações pendentes

O acesso de automação ao navegador não ficou disponível: o host de navegador estava indisponível, e a tentativa de acesso ao Chrome expirou na autorização da ferramenta. Por isso, a aparência, as interações visuais e os tamanhos de tela precisam de conferência manual no navegador. A interface foi construída com CSS mobile first, mas não há uma validação visual concluída desta entrega.

Não havia instância PostgreSQL/Docker disponível para teste. O perfil PostgreSQL, o driver e as migrações foram incluídos; os testes transacionais executados usam H2 em modo PostgreSQL.

Para conferir manualmente: execute `scripts/executar.ps1 -Demo` numa base nova, entre com `admin`, abra uma OS, designe `tecnico`, inicie, registre material, salve relato e conclua com assinatura. Confira também a navegação em 360px de largura, o formulário de assinatura por toque e as permissões ao entrar como técnico.
