package br.com.fluxoos.entity;

import br.com.fluxoos.enums.TipoMovimento;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "movimentos")
public class MovimentoEstoque {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "movimento_ids")
    @SequenceGenerator(name = "movimento_ids", sequenceName = "sequencia_ids", allocationSize = 1)
    private Long id;
    @Column(name = "material_id", nullable = false)
    private long materialId;
    @Column(name = "estoque_id")
    private Long estoqueId;
    @Column(name = "usuario_id", nullable = false)
    private long usuarioId;
    @Column(name = "ordem_id")
    private Long ordemServicoId;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoMovimento tipoMovimento;
    @Column(nullable = false)
    private int variacao;
    @Column(name = "saldo_apos", nullable = false)
    private int saldoApos;
    @Column(nullable = false, length = 500)
    private String observacao;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    protected MovimentoEstoque() {}

    public MovimentoEstoque(Material material, Usuario usuario, OrdemServico ordem,
                            TipoMovimento tipo, int variacao, int saldoApos, String observacao) {
        this.materialId = material.getId();
        this.estoqueId = material.getEstoqueId();
        this.usuarioId = usuario.getId();
        this.ordemServicoId = ordem == null ? null : ordem.getId();
        this.tipoMovimento = Objects.requireNonNull(tipo, "Tipo obrigatório.");
        this.variacao = variacao;
        this.saldoApos = Validacoes.naoNegativo(saldoApos, "Saldo após movimento");
        this.observacao = Validacoes.textoObrigatorio(observacao, "Observação");
    }

    public String descreverEvento() {
        return "Movimentação " + tipoMovimento + " material=" + materialId
                + " variacao=" + variacao + " saldoApos=" + saldoApos
                + " usuario=" + usuarioId + " OS=" + ordemServicoId + " estoque=" + estoqueId;
    }

    public long getId() { return id; }
    public long getMaterialId() { return materialId; }
    public Long getEstoqueId() { return estoqueId; }
    public long getUsuarioId() { return usuarioId; }
    public Long getOrdemServicoId() { return ordemServicoId; }
    public TipoMovimento getTipoMovimento() { return tipoMovimento; }
    public int getVariacao() { return variacao; }
    public int getSaldoApos() { return saldoApos; }
    public String getObservacao() { return observacao; }
    public Instant getCriadoEm() { return criadoEm; }
}
