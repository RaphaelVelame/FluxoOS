package br.com.fluxoos.infrastructure.persistencia;

import br.com.fluxoos.application.porta.RepositorioEventosSeguranca;
import br.com.fluxoos.domain.seguranca.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class EventosSegurancaJdbc implements RepositorioEventosSeguranca {
    private final JdbcTemplate jdbc;
    public EventosSegurancaJdbc(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public EventoSeguranca registrar(TipoEventoSeguranca tipo, String detalhes, Long autorId, Long alvoId, Instant quando) {
        long id = jdbc.queryForObject("SELECT nextval('sequencia_ids')", Long.class);
        jdbc.update("INSERT INTO auditoria (id,usuario_id,tipo,detalhes,criado_em) VALUES (?,?,?,?,?)",
                id, autorId, tipo.name(), detalhes, Timestamp.from(quando));
        return new EventoSeguranca(id, tipo, detalhes, autorId, alvoId, quando);
    }

    public List<EventoSeguranca> listar() {
        String tipos = "'LOGIN_SUCESSO','LOGIN_FALHA','LOGIN_BLOQUEADO','SESSAO_REVOGADA','ACESSO_NEGADO','CSRF_INVALIDO','SENHA_ALTERADA'";
        return jdbc.query("SELECT * FROM auditoria WHERE tipo IN (" + tipos + ") ORDER BY id DESC", (r, n) ->
                new EventoSeguranca(r.getLong("id"), TipoEventoSeguranca.valueOf(r.getString("tipo")), r.getString("detalhes"),
                        r.getObject("usuario_id", Long.class), null, r.getTimestamp("criado_em").toInstant()));
    }
}
