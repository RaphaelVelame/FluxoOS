package br.com.fluxoos.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auditoria")
public class EventoAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auditoria_ids")
    @SequenceGenerator(name = "auditoria_ids", sequenceName = "sequencia_ids", allocationSize = 1)
    private Long id;
    @Column(name = "ordem_id")
    private Long ordemId;
    @Column(name = "usuario_id")
    private Long usuarioId;
    @Column(name = "alvo_id")
    private Long alvoId;
    @Column(nullable = false, length = 60)
    private String tipo;
    @Column(nullable = false, length = 1000)
    private String detalhes;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected EventoAuditoria() {}

    public EventoAuditoria(Long ordemId, Long usuarioId, Long alvoId, String tipo, String detalhes, Instant criadoEm) {
        this.ordemId = ordemId;
        this.usuarioId = usuarioId;
        this.alvoId = alvoId;
        this.tipo = Validacoes.textoObrigatorio(tipo, "Tipo do evento");
        this.detalhes = Validacoes.textoObrigatorio(detalhes, "Detalhes");
        this.criadoEm = criadoEm;
    }

    public long getId() { return id; }
    public Long getOrdemId() { return ordemId; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getAlvoId() { return alvoId; }
    public String getTipo() { return tipo; }
    public String getDetalhes() { return detalhes; }
    public Instant getCriadoEm() { return criadoEm; }
}
