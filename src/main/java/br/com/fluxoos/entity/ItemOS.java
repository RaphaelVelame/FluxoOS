package br.com.fluxoos.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Embeddable
public class ItemOS {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;
    @Column(name = "quantidade", nullable = false)
    private int quantidadeUsada;
    @Column(name = "estoque_id")
    private Long estoqueId;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    protected ItemOS() {}

    public ItemOS(Material material, int quantidadeUsada) {
        this.material = Objects.requireNonNull(material, "Material obrigatório.");
        this.quantidadeUsada = Validacoes.positivo(quantidadeUsada, "Quantidade usada");
        this.estoqueId = material.getEstoqueId();
        this.empresaId = material.getEmpresaId();
    }

    public Material getMaterial() { return material; }
    public int getQuantidadeUsada() { return quantidadeUsada; }
    public Long getEstoqueId() { return estoqueId; }
}
