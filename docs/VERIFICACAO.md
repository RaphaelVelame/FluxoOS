# Verificação da entrega

Verificado em 05/11/2026, Windows, JDK 25, com compilação direcionada ao Java 17.

- Maven Wrapper 3.9.11: `clean test` e, após a revisão final, `verify`, ambos concluídos com sucesso.
- 17 testes passaram: 12 operacionais/de segurança, um de separação dos controllers, um de proteção de dados sensíveis nos DTOs, um de carga inicial e dois de firewall HTTP.
- `StrictHttpFirewall` aplicado antes da autenticação: parâmetros de caminho com ponto e vírgula literal ou codificado retornam 400 no servidor real; página pública, login e consultas autenticadas continuam funcionando.
- Entidades e repositories JPA validados pelo Hibernate contra as migrações Flyway V1 e V2 em H2 em memória.
- Integração com PostgreSQL 18.6 local: banco e usuário `fluxoos`, credenciais no `.env` ignorado pelo Git e conexão verificada pelo `psql`.
- Flyway atualizado para 11.20.0; migrações V1 e V2 aplicadas no PostgreSQL sem aviso de versão não reconhecida.
- Os mesmos 12 testes operacionais passaram no PostgreSQL real, em esquema temporário isolado; o esquema foi removido e `public` permaneceu vazio antes da inicialização definitiva.
- Scripts de conexão e teste PostgreSQL executados com sucesso no Windows PowerShell 5.1. O teste também verificou a remoção do esquema após falha de compilação por bloqueio de download.
- CI configurado para repetir os 12 testes operacionais em um serviço PostgreSQL 18 descartável, além dos 17 testes da suíte padrão.
- Aplicação iniciada em `http://localhost:8080` com perfil `postgres`, migrações V1/V2 confirmadas no esquema `public` e conta administrativa persistida. Página inicial, login, consultas autenticadas de usuários/ordens/materiais e logout retornaram HTTP 200. Credenciais iniciais disponíveis apenas no arquivo local ignorado `data/acesso-inicial.txt`.
- Fluxo HTTP exercitado com MockMvc: login, cookie HttpOnly, CSRF, permissões, abertura, atribuição, consumo, relato e conclusão com assinatura.
- Rollback, consumo concorrente, repetição de requisições e proteção contra sobrescrita de chaves de idempotência verificados.
- Troca de senha com revogação de sessões e desativação de contas verificadas.
- Carga demonstrativa criou três ordens, três materiais, três movimentos de entrada e duas contas; uma segunda execução preservou as quantidades.
- JAR Spring Boot empacotado com Spring Data JPA/Hibernate e configuração YAML.
- Corrigida a leitura de diretórios sem destino de link no Maven Wrapper para Windows.

## Verificações pendentes

A interface estática não foi alterada nesta migração. A aparência, as interações visuais e os tamanhos de tela precisam de conferência manual no navegador; os testes HTTP não substituem essa validação.

Backup/restauração e carga prolongada no PostgreSQL ainda precisam de ensaio no ambiente de implantação.

Para conferir manualmente: execute `scripts/executar.ps1 -Demo` numa base nova, entre com `admin`, abra uma OS, designe `tecnico`, inicie, registre material, salve relato e conclua com assinatura. Confira também a navegação em 360px de largura, o formulário de assinatura por toque e as permissões ao entrar como técnico.
