package br.com.fluxoos.domain.modelo;

import java.util.Objects;

public final class EventoAtribuicao extends RegistroBase {
    private final Long equipeAnteriorId; 
    private final long equipeNovaId;
    private final long feitoPorId;

    public EventoAtribuicao(Long equipeAnteriorId, Usuario equipeNova, Usuario feitoPor) {
        super();
        this.equipeAnteriorId = equipeAnteriorId;
        this.equipeNovaId = Objects.requireNonNull(equipeNova, "Equipe nova obrigatória.").getId();
        this.feitoPorId = Objects.requireNonNull(feitoPor, "Responsável obrigatório.").getId();
        if (equipeAnteriorId != null && equipeAnteriorId == equipeNovaId) {
            throw new IllegalArgumentException("Equipe nova deve ser diferente da anterior.");
        }
    }

    @Override
    public String descreverEvento() {
        return "Atribuição alterada: " + (equipeAnteriorId == null ? "nenhuma" : equipeAnteriorId)
                + " -> " + equipeNovaId + " por usuário=" + feitoPorId + " em " + getCriadoEm();
    }

    public Long getEquipeAnteriorId() { return equipeAnteriorId; }
    public long getEquipeNovaId() { return equipeNovaId; }
    public long getFeitoPorId() { return feitoPorId; }
}
