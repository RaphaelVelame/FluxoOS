package br.com.fluxoos.domain.modelo;

public enum Perfil {
    CHEFE(true),
    SUPORTE_TI(true),
    TECNICO(false),
    SOLICITANTE(false);

    private final boolean podeMovimentarEstoque;

    Perfil(boolean podeMovimentarEstoque) {
        this.podeMovimentarEstoque = podeMovimentarEstoque;
    }

    public boolean podeMovimentarEstoque() {
        return podeMovimentarEstoque;
    }
}
