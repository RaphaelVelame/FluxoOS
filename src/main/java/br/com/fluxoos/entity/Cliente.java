package br.com.fluxoos.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "clientes", uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "codigo"}))
public class Cliente extends RegistroEmpresa {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cliente_ids")
    @SequenceGenerator(name = "cliente_ids", sequenceName = "sequencia_ids", allocationSize = 1)
    private Long id;
    @Column(nullable = false, length = 60)
    private String codigo;
    @Column(nullable = false, length = 160)
    private String nome;

    protected Cliente() {}

    public Cliente(long empresaId, String codigo, String nome) {
        vincularEmpresa(empresaId);
        this.codigo = Validacoes.textoObrigatorio(codigo, "Código do cliente");
        this.nome = Validacoes.textoObrigatorio(nome, "Nome do cliente");
        if (this.codigo.length() > 60 || this.nome.length() > 160)
            throw new IllegalArgumentException("Código ou nome do cliente excede o limite permitido.");
    }

    public long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
}
