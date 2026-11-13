package br.com.fluxoos.entity;

import br.com.fluxoos.enums.TipoEstoque;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "estoques")
public class Estoque extends RegistroEmpresa {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "estoque_ids")
    @SequenceGenerator(name = "estoque_ids", sequenceName = "sequencia_ids", allocationSize = 1)
    private Long id;
    @Column(nullable = false, unique = true, length = 60)
    private String codigo;
    @Column(nullable = false, length = 160)
    private String nome;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEstoque tipo;

    protected Estoque() {}

    public Estoque(String codigo, String nome, TipoEstoque tipo) {
        this.codigo = Validacoes.textoObrigatorio(codigo, "Código do estoque");
        this.nome = Validacoes.textoObrigatorio(nome, "Nome do estoque");
        this.tipo = Objects.requireNonNull(tipo, "Tipo do estoque obrigatório.");
    }
    public long getId() { return id; }
    public Estoque(long empresaId, String codigo, String nome, TipoEstoque tipo) {
        this(codigo, nome, tipo); vincularEmpresa(empresaId);
    }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public TipoEstoque getTipo() { return tipo; }
}
