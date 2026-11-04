package br.com.fluxoos.application.seguranca;

/** Mensagem uniforme: não revela usuário inexistente, senha errada ou bloqueio. */
public final class AutenticacaoNegadaException extends RuntimeException {
    public AutenticacaoNegadaException() { super("Não foi possível autenticar com as credenciais informadas."); }
}
