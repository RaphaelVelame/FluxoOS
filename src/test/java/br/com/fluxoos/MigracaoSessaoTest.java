package br.com.fluxoos;

import br.com.fluxoos.seguranca.SenhaPbkdf2;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class MigracaoSessaoTest {
    private static final String HASH_VALIDO = new SenhaPbkdf2().gerarHash("Senha-legada-preservada-2026!".toCharArray());
    @Test void atualizaBancoV2PreservandoContasExistentes() throws Exception {
        String url = System.getenv().getOrDefault("TEST_DATABASE_URL",
                "jdbc:h2:mem:migracao-sessao;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        String usuario = System.getenv().getOrDefault("TEST_DATABASE_USER", "sa");
        String senha = System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", "");
        String schema = "migracao_" + UUID.randomUUID().toString().replace("-", "");
        try (var conexao = DriverManager.getConnection(url, usuario, senha)) {
            try {
                MigracoesTeste.configurar(url, usuario, senha, schema)
                        .target("2").load().migrate();
                conexao.setSchema(schema);
                try (var sql = conexao.createStatement()) {
                    sql.executeUpdate("INSERT INTO usuarios (id,nome_usuario,nome_completo,perfil,ativo,hash_senha) "
                            + "VALUES (1,'existente','Conta existente','CHEFE',true,'" + HASH_VALIDO + "')");
                    sql.executeUpdate("INSERT INTO ordens (id,solicitante,local,problema,prioridade,situacao) "
                            + "VALUES (2,'Ana','Escola','Rede','ALTA','ABERTA')");
                }
                MigracoesTeste.configurar(url, usuario, senha, schema).load().migrate();
                try (var sql = conexao.createStatement();
                     var linha = sql.executeQuery("SELECT nome_usuario,hash_senha,ativo,versao_sessao FROM usuarios WHERE id=1")) {
                    assertThat(linha.next()).isTrue();
                    assertThat(linha.getString(1)).isEqualTo("existente");
                    assertThat(linha.getString(2)).isEqualTo(HASH_VALIDO);
                    assertThat(linha.getBoolean(3)).isTrue();
                    assertThat(linha.getLong(4)).isZero();
                    assertThat(linha.next()).isFalse();
                }
                try (var sql = conexao.createStatement(); var linha = sql.executeQuery(
                        "SELECT problema,chave_abertura,autor_abertura_id,conteudo_abertura FROM ordens WHERE id=2")) {
                    assertThat(linha.next()).isTrue();
                    assertThat(linha.getString(1)).isEqualTo("Rede");
                    assertThat(linha.getString(2)).isNull();
                    assertThat(linha.getObject(3)).isNull();
                    assertThat(linha.getString(4)).isNull();
                }
            } finally {
                try (var sql = conexao.createStatement()) { sql.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE"); }
            }
        }
    }
}
