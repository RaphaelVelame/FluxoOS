package br.com.fluxoos.dto;

import java.time.Instant;

/** Segredos retornados APENAS no login. Não registrar em log nem imprimir! */
public final class TokensEmitidos {
    private final String tokenSessao;
    private final String tokenCsrf;
    private final Instant expiraEm;

    public TokensEmitidos(String tokenSessao, String tokenCsrf, Instant expiraEm) {
        this.tokenSessao = tokenSessao;
        this.tokenCsrf = tokenCsrf;
        this.expiraEm = expiraEm;
    }
    public String tokenSessao() { return tokenSessao; }
    public String tokenCsrf() { return tokenCsrf; }
    public Instant expiraEm() { return expiraEm; }
    @Override public String toString() { return "TokensEmitidos{tokens=[REDACTED], expiraEm=" + expiraEm + "}"; }
}
