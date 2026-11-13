package br.com.fluxoos.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "materiais")
public class Material extends RegistroEmpresa {
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
    @Column(name = "referencia_fabricante", length = 160)
    private String referenciaFabricante;
    @Column(nullable = false)
    private int quantidade;
    @Column(name = "quantidade_minima", nullable = false)
    private int quantidadeMinima;
    @Column(nullable = false)
    private boolean ativo;
    @Column(name = "estoque_id")
    private Long estoqueId;

    protected Material() {}

    public Material(String codigo, String nome, String unidade,
                    int quantidade, int quantidadeMinima, boolean ativo) {
        this(codigo, nome, unidade, quantidade, quantidadeMinima, ativo, null);
    }

    public Material(String codigo, String nome, String unidade,
                    int quantidade, int quantidadeMinima, boolean ativo, Long estoqueId) {
        this.codigo = Validacoes.textoObrigatorio(codigo, "Código");
        this.nome = Validacoes.textoObrigatorio(nome, "Nome do material");
        this.unidade = Validacoes.textoObrigatorio(unidade, "Unidade");
        this.quantidade = Validacoes.naoNegativo(quantidade, "Saldo inicial");
        this.quantidadeMinima = Validacoes.naoNegativo(quantidadeMinima, "Quantidade mínima");
        this.ativo = ativo;
        this.estoqueId = estoqueId;
    }

    public long getId() { return id; }
    public Material(long empresaId, String codigo, String nome, String unidade, int minimo, Long estoqueId) {
        this(codigo, nome, unidade, 0, minimo, true, estoqueId);
        vincularEmpresa(empresaId);
    }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public String getUnidade() { return unidade; }
    public String getReferenciaFabricante() { return referenciaFabricante; }
    public int getQuantidade() { return quantidade; }
    public int getQuantidadeMinima() { return quantidadeMinima; }
    public boolean isAtivo() { return ativo; }
    public Long getEstoqueId() { return estoqueId; }

    public boolean classificarEstoque(long destino) {
        if (destino <= 0) throw new IllegalArgumentException("Estoque inválido.");
        if (java.util.Objects.equals(estoqueId, destino)) return false;
        if (estoqueId != null) throw new IllegalStateException("O material já pertence a um estoque. Uma mudança de proprietário exige transferência de estoque.");
        estoqueId = destino;
        return true;
    }

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
