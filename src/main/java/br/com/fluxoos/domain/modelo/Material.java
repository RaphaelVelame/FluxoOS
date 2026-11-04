package br.com.fluxoos.domain.modelo;


public final class Material {
    private final long id;
    private final String codigo;
    private final String nome;
    private final String unidade;
    private int quantidade;
    private final int quantidadeMinima;
    private boolean ativo;

    public Material(long id, String codigo, String nome, String unidade,
                    int quantidade, int quantidadeMinima, boolean ativo) {
        this.id = Validacoes.idPositivo(id, "ID do material");
        this.codigo = Validacoes.textoObrigatorio(codigo, "Código");
        this.nome = Validacoes.textoObrigatorio(nome, "Nome do material");
        this.unidade = Validacoes.textoObrigatorio(unidade, "Unidade");
        this.quantidade = Validacoes.naoNegativo(quantidade, "Saldo inicial");
        this.quantidadeMinima = Validacoes.naoNegativo(quantidadeMinima, "Quantidade mínima");
        this.ativo = ativo;
    }

    public long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public String getUnidade() { return unidade; }
    public int getQuantidade() { return quantidade; }
    public int getQuantidadeMinima() { return quantidadeMinima; }
    public boolean isAtivo() { return ativo; }

    public boolean isAbaixoDoMinimo() {
        return quantidade < quantidadeMinima;
    }

    int preverSaldo(int variacao) {
        if (!ativo) {
            throw new IllegalStateException("Material inativo não pode ser movimentado.");
        }
        final int novoSaldo;
        try {
            novoSaldo = Math.addExact(quantidade, variacao);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Movimentação ultrapassa o limite numérico.", e);
        }
        if (novoSaldo < 0) {
            throw new IllegalStateException("Estoque insuficiente para o material " + codigo + ".");
        }
        return novoSaldo;
    }

    int aplicarVariacao(int variacao) {
        quantidade = preverSaldo(variacao);
        return quantidade;
    }

    void desativar() { ativo = false; }
    void ativar() { ativo = true; }
}
