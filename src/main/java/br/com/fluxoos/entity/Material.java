package br.com.fluxoos.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "materiais")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "material_ids")
    @SequenceGenerator(name = "material_ids", sequenceName = "sequencia_ids", allocationSize = 1)
    private Long id;
    @Column(nullable = false, unique = true, length = 60)
    private String codigo;
    @Column(nullable = false, length = 160)
    private String nome;
    @Column(nullable = false, length = 20)
    private String unidade;
    @Column(nullable = false)
    private int quantidade;
    @Column(name = "quantidade_minima", nullable = false)
    private int quantidadeMinima;
    @Column(nullable = false)
    private boolean ativo;

    protected Material() {}

    public Material(String codigo, String nome, String unidade,
                    int quantidade, int quantidadeMinima, boolean ativo) {
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

    public int preverSaldo(int variacao) {
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

    public int aplicarVariacao(int variacao) {
        quantidade = preverSaldo(variacao);
        return quantidade;
    }

    public void desativar() { ativo = false; }
    public void ativar() { ativo = true; }
}
