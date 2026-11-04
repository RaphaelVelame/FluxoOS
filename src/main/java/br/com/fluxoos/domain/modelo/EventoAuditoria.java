package br.com.fluxoos.domain.modelo;

import java.util.Objects;

public final class EventoAuditoria extends RegistroBase {
    private final String tipoEvento;
    private final String detalhes;
    private final long executadoPorId;

    public EventoAuditoria(String tipoEvento, String detalhes, Usuario executadoPor) {
        super();
        this.tipoEvento = Validacoes.textoObrigatorio(tipoEvento, "Tipo do evento");
        this.detalhes = Validacoes.textoObrigatorio(detalhes, "Detalhes");
        this.executadoPorId = Objects.requireNonNull(executadoPor, "Autor obrigatório.").getId();
    }

    @Override
    public String descreverEvento() {
        return "Auditoria [" + tipoEvento + "] usuário=" + executadoPorId + " em " + getCriadoEm();
    }

    public String getTipoEvento() { return tipoEvento; }
    public String getDetalhes() { return detalhes; }
    public long getExecutadoPorId() { return executadoPorId; }
}
