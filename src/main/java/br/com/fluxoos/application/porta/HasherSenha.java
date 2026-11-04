package br.com.fluxoos.application.porta;

/** Interface para permitir troca por Argon2id usando uma biblioteca confiável. */
public interface HasherSenha {
    String gerarHash(char[] senha);
    boolean verificar(char[] senha, String hashArmazenado);
}
