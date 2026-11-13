package br.com.fluxoos.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notificacoes", uniqueConstraints = @UniqueConstraint(columnNames = {"ordem_id", "destinatario_id"}))
public class Notificacao extends RegistroEmpresa {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notificacao_ids")
    @SequenceGenerator(name = "notificacao_ids", sequenceName = "sequencia_ids", allocationSize = 1)
    private Long id;
    @Column(name = "ordem_id", nullable = false, updatable = false) private long ordemId;
    @Column(name = "destinatario_id", nullable = false, updatable = false) private long destinatarioId;
    @Column(nullable = false, length = 240) private String mensagem;
    @Column(name = "criado_em", nullable = false, updatable = false) private Instant criadoEm;
    @Column(nullable = false) private boolean lida;
    protected Notificacao() {}
    public Notificacao(long empresaId, long ordemId, long destinatarioId, Instant agora) {
        vincularEmpresa(empresaId); this.ordemId = ordemId; this.destinatarioId = destinatarioId;
        mensagem = "Nova ordem de serviço #" + ordemId + " criada pela equipe.";
        criadoEm = agora;
    }
    public long getId() { return id; }
    public long getOrdemId() { return ordemId; }
    public String getMensagem() { return mensagem; }
    public Instant getCriadoEm() { return criadoEm; }
    public boolean isLida() { return lida; }
    public void marcarLida() { lida = true; }
}
