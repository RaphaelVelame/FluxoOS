package br.com.fluxoos.infrastructure.seguranca;

import br.com.fluxoos.application.porta.HasherSenha;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PBKDF2-HMAC-SHA256 com 600.000 iterações (padrão de demonstração compatível
 * com recomendação OWASP para PBKDF2). Para novos sistemas, prefira Argon2id.
 */
public final class SenhaPbkdf2 implements HasherSenha {
    private static final int ITERACOES = 600_000;
    private static final int BITS = 256;
    private final SecureRandom random = new SecureRandom();

    @Override public String gerarHash(char[] senha) {
        exigirSenha(senha);
        if (senha.length < 15) {
            throw new IllegalArgumentException("Nova senha deve ter pelo menos 15 caracteres sem MFA");
        }
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        byte[] resultado = derivar(senha, salt, ITERACOES);
        return "pbkdf2_sha256$" + ITERACOES + "$"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(salt)
                + "$" + Base64.getUrlEncoder().withoutPadding().encodeToString(resultado);
    }

    @Override public boolean verificar(char[] senha, String hashArmazenado) {
        exigirSenha(senha);
        try {
            String[] campos = hashArmazenado.split("\\$", -1);
            if (campos.length != 4 || !campos[0].equals("pbkdf2_sha256")) return false;
            int iteracoes = Integer.parseInt(campos[1]);
            if (iteracoes < ITERACOES || iteracoes > 2_000_000) return false;
            byte[] salt = Base64.getUrlDecoder().decode(campos[2]);
            byte[] esperado = Base64.getUrlDecoder().decode(campos[3]);
            if (salt.length != 16 || esperado.length != BITS / 8) return false;
            return MessageDigest.isEqual(esperado, derivar(senha, salt, iteracoes));
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    private static void exigirSenha(char[] senha) {
        if (senha == null || senha.length == 0 || senha.length > 1024) {
            throw new IllegalArgumentException("Senha inválida");
        }
    }

    private static byte[] derivar(char[] senha, byte[] salt, int iteracoes) {
        PBEKeySpec spec = new PBEKeySpec(senha, salt, iteracoes, BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Derivação de senha indisponível", e);
        } finally {
            spec.clearPassword();
        }
    }
}
