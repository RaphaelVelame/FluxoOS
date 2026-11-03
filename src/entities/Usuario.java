package entities;

import java.util.Objects;

public final class Usuario {
    private final long id;
    private final String nomeUsuario;
    private final String nomeCompleto;
    private final Perfil perfil;
    private boolean ativo;

    public Usuario(long id, String nomeUsuario, String nomeCompleto, Perfil perfil, boolean ativo) {
        this.id = Validacoes.idPositivo(id, "ID do usuário");
        this.nomeUsuario = Validacoes.textoObrigatorio(nomeUsuario, "Nome de usuário");
        this.nomeCompleto = Validacoes.textoObrigatorio(nomeCompleto, "Nome completo");
        this.perfil = Objects.requireNonNull(perfil, "Perfil é obrigatório.");
        this.ativo = ativo;
    }

    public long getId() { return id; }
    public String getNomeUsuario() { return nomeUsuario; }
    public String getNomeCompleto() { return nomeCompleto; }
    public Perfil getPerfil() { return perfil; }
    public boolean isAtivo() { return ativo; }

    public void ativar() { ativo = true; }
    public void desativar() { ativo = false; }

    public boolean podeMovimentarEstoque() {
        return ativo && perfil.podeMovimentarEstoque();
    }
}
