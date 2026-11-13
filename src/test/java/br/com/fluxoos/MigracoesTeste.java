package br.com.fluxoos;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

final class MigracoesTeste {
    private MigracoesTeste() {}

    static FluentConfiguration configurar(String url, String usuario, String senha, String schema) {
        String banco = url.startsWith("jdbc:postgresql:") ? "postgresql" : "h2";
        if (banco.equals("postgresql") && !"true".equals(System.getenv("TEST_DATABASE_DISPOSABLE"))) {
            throw new IllegalStateException("Use PostgreSQL descartavel e TEST_DATABASE_DISPOSABLE=true.");
        }
        return Flyway.configure().dataSource(url, usuario, senha).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration", "classpath:db/" + banco);
    }
}
