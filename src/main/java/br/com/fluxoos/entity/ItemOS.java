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

    protected ItemOS() {}

    public ItemOS(Material material, int quantidadeUsada) {
        this.material = Objects.requireNonNull(material, "Material obrigatório.");
        this.quantidadeUsada = Validacoes.positivo(quantidadeUsada, "Quantidade usada");
    }

    public Material getMaterial() { return material; }
    public int getQuantidadeUsada() { return quantidadeUsada; }
}
