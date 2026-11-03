package entities;

public enum TipoMovimento {
    ENTRADA("Entrada de material"),
    SAIDA("Consumo na ordem de serviço"),
    DEVOLUCAO("Devolução ao estoque"),
    AJUSTE("Ajuste de inventário");

    private final String descricao;

    TipoMovimento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
