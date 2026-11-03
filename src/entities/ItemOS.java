package entities;

import java.util.Objects;

public final class ItemOS {
    private final Material material;
    private final int quantidadeUsada;

    ItemOS(Material material, int quantidadeUsada) {
        this.material = Objects.requireNonNull(material, "Material obrigatório.");
        this.quantidadeUsada = Validacoes.positivo(quantidadeUsada, "Quantidade usada");
    }

    public Material getMaterial() { return material; }
    public int getQuantidadeUsada() { return quantidadeUsada; }
}
