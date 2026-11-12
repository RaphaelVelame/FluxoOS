package br.com.fluxoos.seguranca;

import br.com.fluxoos.seguranca.PoliticaBloqueio;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Herança: centraliza a lógica compartilhada pelos dois controles de falhas. */
public abstract class ControleTentativas {
    private final Deque<Instant> instantesFalha = new ArrayDeque<>();
    private Instant ultimaFalha;
    private Instant bloqueadoAte;

    public final synchronized void registrarFalha(Instant agora, PoliticaBloqueio politica) {
        Objects.requireNonNull(agora);
        Objects.requireNonNull(politica);
        limparJanela(agora, politica);
        if (bloqueadoAte != null && agora.isBefore(bloqueadoAte)) return;
        instantesFalha.addLast(agora);
        ultimaFalha = agora;
        if (instantesFalha.size() >= politica.limite()) {
            bloqueadoAte = agora.plus(politica.duracaoBloqueio());
        }
    }

    public final synchronized boolean estaBloqueado(Instant agora, PoliticaBloqueio politica) {
        Objects.requireNonNull(agora);
        Objects.requireNonNull(politica);
        limparJanela(agora, politica);
        return bloqueadoAte != null && agora.isBefore(bloqueadoAte);
    }

    /** Zera falhas da identidade após login válido; não zere o IP nesse momento. */
    public final synchronized void registrarSucesso() {
        instantesFalha.clear();
        bloqueadoAte = null;
        ultimaFalha = null;
    }

    public final synchronized int getFalhas() { return instantesFalha.size(); }
    public final synchronized Instant getUltimaFalha() { return ultimaFalha; }
    public final synchronized Instant getBloqueadoAte() { return bloqueadoAte; }

    public final synchronized boolean podeDescartar(Instant agora, PoliticaBloqueio politica) {
        return !estaBloqueado(agora, politica) && instantesFalha.isEmpty();
    }

    private void limparJanela(Instant agora, PoliticaBloqueio politica) {
        if (bloqueadoAte != null && !agora.isBefore(bloqueadoAte)) {
            // Terminado o bloqueio, começa uma nova janela.
            registrarSucesso();
        }
        Instant inicio = agora.minus(politica.janela());
        while (!instantesFalha.isEmpty() && !instantesFalha.peekFirst().isAfter(inicio)) {
            instantesFalha.removeFirst();
        }
        if (instantesFalha.isEmpty()) ultimaFalha = null;
    }
}
