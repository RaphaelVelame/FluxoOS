package br.com.fluxoos.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "empresas")
public class Empresa {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "empresa_ids")
    @SequenceGenerator(name = "empresa_ids", sequenceName = "sequencia_ids", allocationSize = 1)
    private Long id;
    @Column(nullable = false, length = 160)
    private String nome;
    protected Empresa() {}
    public Empresa(String nome) { this.nome = Validacoes.textoObrigatorio(nome, "Empresa"); }
    public long getId() { return id; }
    public String getNome() { return nome; }
}
