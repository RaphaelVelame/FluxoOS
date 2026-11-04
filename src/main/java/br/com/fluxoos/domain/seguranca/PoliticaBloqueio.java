package br.com.fluxoos.domain.seguranca;

import java.time.Duration;
import java.util.Objects;

/** Configuração de limitação por identidade ou endereço IP. */
public record PoliticaBloqueio(int limite, Duration janela, Duration duracaoBloqueio) {
    public PoliticaBloqueio {
        if (limite < 2) throw new IllegalArgumentException("Limite deve ser >= 2");
        Objects.requireNonNull(janela, "Janela obrigatória");
        Objects.requireNonNull(duracaoBloqueio, "Bloqueio obrigatório");
        if (janela.isNegative() || janela.isZero()
                || duracaoBloqueio.isNegative() || duracaoBloqueio.isZero()) {
            throw new IllegalArgumentException("Durações devem ser positivas");
        }
    }
}
