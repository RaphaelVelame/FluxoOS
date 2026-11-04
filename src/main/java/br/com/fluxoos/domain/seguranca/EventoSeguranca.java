package br.com.fluxoos.domain.seguranca;

import br.com.fluxoos.domain.modelo.Auditavel;
import java.time.Instant;
import java.util.Objects;

/** Registro imutável: não guarda senha, token de sessão ou token CSRF. */
public final class EventoSeguranca implements Auditavel {
    private final long id;
    private final TipoEventoSeguranca tipoEvento;
    private final String detalhes;
    private final Long autorId;
    private final Long alvoId;
    private final Instant criadoEm;

    public EventoSeguranca(long id, TipoEventoSeguranca tipoEvento, String detalhes,
                    Long autorId, Long alvoId, Instant criadoEm) {
        if (id <= 0) throw new IllegalArgumentException("ID inválido");
        this.id = id;
        this.tipoEvento = Objects.requireNonNull(tipoEvento);
        if (detalhes == null || detalhes.length() > 120 || detalhes.isBlank()) {
            throw new IllegalArgumentException("Detalhes inválidos");
        }
        this.detalhes = detalhes;
        this.autorId = autorId;
        this.alvoId = alvoId;
        this.criadoEm = Objects.requireNonNull(criadoEm);
    }
    public long getId() { return id; }
    public TipoEventoSeguranca getTipoEvento() { return tipoEvento; }
    public String getDetalhes() { return detalhes; }
    public Long getAutorId() { return autorId; }
    public Long getAlvoId() { return alvoId; }
    public Instant getCriadoEm() { return criadoEm; }
    @Override public String descreverEvento() {
        return "SEGURANCA [" + tipoEvento + "] id=" + id + " em " + criadoEm;
    }
}
