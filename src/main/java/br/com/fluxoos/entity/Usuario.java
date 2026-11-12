package br.com.fluxoos.entity;

import br.com.fluxoos.enums.Perfil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_ids")
    @SequenceGenerator(name = "usuario_ids", sequenceName = "sequencia_ids", allocationSize = 1)
    private Long id;
    @Column(name = "nome_usuario", nullable = false, unique = true, length = 80)
    private String nomeUsuario;
    @Column(name = "nome_completo", nullable = false, length = 160)
    private String nomeCompleto;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Perfil perfil;
    @Column(nullable = false)
    private boolean ativo;
    @JsonIgnore
    @Column(name = "versao_sessao", nullable = false)
    private long versaoSessao;
    @JsonIgnore
    @Column(name = "hash_senha", nullable = false, length = 300)
    private String hashSenha;

    protected Usuario() {}

    public Usuario(String nomeUsuario, String nomeCompleto, Perfil perfil, String hashSenha) {
        this.nomeUsuario = Validacoes.textoObrigatorio(nomeUsuario, "Nome de usuário");
        this.nomeCompleto = Validacoes.textoObrigatorio(nomeCompleto, "Nome completo");
        this.perfil = Objects.requireNonNull(perfil, "Perfil é obrigatório.");
        this.hashSenha = Validacoes.textoObrigatorio(hashSenha, "Hash da senha");
        this.ativo = true;
    }

    public long getId() { return id; }
    public String getNomeUsuario() { return nomeUsuario; }
    public String getNomeCompleto() { return nomeCompleto; }
    public Perfil getPerfil() { return perfil; }
    public boolean isAtivo() { return ativo; }
    public long getVersaoSessao() { return versaoSessao; }

    public void ativar() { ativo = true; }
    public void desativar() {
        if (ativo) versaoSessao = Math.incrementExact(versaoSessao);
        ativo = false;
    }

    public boolean podeMovimentarEstoque() {
        return ativo && perfil.podeMovimentarEstoque();
    }
}
