package br.com.fluxoos.infrastructure.persistencia;

import br.com.fluxoos.application.porta.RepositorioUsuarios;
import br.com.fluxoos.application.porta.RepositorioCredenciais;
import br.com.fluxoos.application.seguranca.Identificadores;
import br.com.fluxoos.domain.modelo.Perfil;
import br.com.fluxoos.domain.modelo.Usuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class UsuariosJdbc implements RepositorioUsuarios, RepositorioCredenciais, br.com.fluxoos.application.porta.AdministracaoUsuarios {
    private final JdbcTemplate jdbc;
    public UsuariosJdbc(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<Usuario> buscarPorNome(String nome) {
        return jdbc.query("SELECT * FROM usuarios WHERE nome_usuario=?", UsuariosJdbc::mapear,
                Identificadores.normalizarUsuario(nome)).stream().findFirst();
    }

    public Optional<Usuario> buscarPorId(long id) {
        return jdbc.query("SELECT * FROM usuarios WHERE id=?", UsuariosJdbc::mapear, id).stream().findFirst();
    }

    public Optional<String> buscarHashSenha(long usuarioId) {
        return jdbc.query("SELECT hash_senha FROM usuarios WHERE id=?", (r, n) -> r.getString(1), usuarioId).stream().findFirst();
    }

    public boolean substituirHashSenha(long usuarioId, String atual, String novo) {
        return jdbc.update("UPDATE usuarios SET hash_senha=? WHERE id=? AND hash_senha=?", novo, usuarioId, atual) == 1;
    }

    public void inserir(Usuario u, String hash) {
        jdbc.update("INSERT INTO usuarios (id,nome_usuario,nome_completo,perfil,ativo,hash_senha) VALUES (?,?,?,?,?,?)",
                u.getId(), Identificadores.normalizarUsuario(u.getNomeUsuario()), u.getNomeCompleto(), u.getPerfil().name(), u.isAtivo(), hash);
    }

    public void alterarStatus(long id, boolean ativo) {
        jdbc.update("UPDATE usuarios SET ativo=? WHERE id=?", ativo, id);
    }

    static Usuario mapear(ResultSet r, int n) throws SQLException {
        return new Usuario(r.getLong("id"), r.getString("nome_usuario"), r.getString("nome_completo"),
                Perfil.valueOf(r.getString("perfil")), r.getBoolean("ativo"));
    }
}
