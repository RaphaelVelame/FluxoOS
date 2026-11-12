package br.com.fluxoos.seguranca;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Sessão opaca persistida no servidor. Guarda hash do identificador e hash CSRF,
 * nunca os valores secretos apresentados ao navegador.
 */
public final class Sessao {
    private final String hashToken;
    private final String hashTokenCsrf;
    private final long usuarioId;
    private final long versaoUsuario;
    private final Instant criadaEm;
    private final Instant expiraEm;
    private final Duration limiteInatividade;
    private Instant ultimoAcesso;
    private Instant revogadaEm;

    public Sessao(String hashToken, String hashTokenCsrf, long usuarioId, long versaoUsuario,
           Instant criadaEm, Duration tempoAbsoluto, Duration limiteInatividade) {
        this.hashToken = Objects.requireNonNull(hashToken);
        this.hashTokenCsrf = Objects.requireNonNull(hashTokenCsrf);
        if (usuarioId <= 0) throw new IllegalArgumentException("ID inválido");
        this.usuarioId = usuarioId;
        if (versaoUsuario < 0) throw new IllegalArgumentException("Versão inválida");
        this.versaoUsuario = versaoUsuario;
        this.criadaEm = Objects.requireNonNull(criadaEm);
        if (tempoAbsoluto.isZero() || tempoAbsoluto.isNegative()
                || limiteInatividade.isZero() || limiteInatividade.isNegative()) {
            throw new IllegalArgumentException("Duração inválida");
        }
        this.expiraEm = criadaEm.plus(tempoAbsoluto);
        this.limiteInatividade = limiteInatividade;
        this.ultimoAcesso = criadaEm;
    }

    public synchronized boolean estaValida(Instant agora) {
        return revogadaEm == null && agora.isBefore(expiraEm)
                && agora.isBefore(ultimoAcesso.plus(limiteInatividade));
    }
    public synchronized boolean acessar(Instant agora) {
        if (!estaValida(agora)) return false;
        if (agora.isAfter(ultimoAcesso)) ultimoAcesso = agora;
        return true;
    }
    public synchronized void revogar(Instant agora) {
        if (revogadaEm == null) revogadaEm = agora;
    }
    public boolean conferirCsrf(String token) { return TokenSeguro.iguais(hashTokenCsrf, token); }

    public String getHashToken() { return hashToken; }
    public long getUsuarioId() { return usuarioId; }
    public long getVersaoUsuario() { return versaoUsuario; }
    public Instant getExpiraEm() { return expiraEm; }
    public Instant getCriadaEm() { return criadaEm; }
    public synchronized Instant getUltimoAcesso() { return ultimoAcesso; }
    public synchronized boolean isRevogada() { return revogadaEm != null; }

    @Override public String toString() {
        return "Sessao{usuarioId=" + usuarioId + ", expiraEm=" + expiraEm + ", tokens=[REDACTED]}";
    }
}
