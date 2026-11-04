package br.com.fluxoos.application.seguranca;

/** Contador por nome de usuário normalizado, inclusive para nomes inexistentes. */
public final class TentativaLogin extends ControleTentativas {
    private final String nomeUsuario;
    public TentativaLogin(String nomeUsuario) {
        this.nomeUsuario = Identificadores.normalizarUsuario(nomeUsuario);
    }
    public String getNomeUsuario() { return nomeUsuario; }
}
