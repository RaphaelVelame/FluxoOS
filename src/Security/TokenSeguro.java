package seguranca;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** SHA-256 é adequado para tokens criptograficamente aleatórios, NÃO para senhas. */
final class TokenSeguro {
    private static final SecureRandom RANDOM = new SecureRandom();
    private TokenSeguro() {}

    static String novoToken() {
        byte[] dados = new byte[32]; // 256 bits
        RANDOM.nextBytes(dados);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(dados);
    }
    static String hash(String token) {
        if (token == null || token.length() != 43 || !token.matches("[A-Za-z0-9_-]{43}")) {
            throw new IllegalArgumentException("Token malformado");
        }
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(resumo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
    static boolean iguais(String hashEsperado, String tokenInformado) {
        try {
            byte[] a = Base64.getUrlDecoder().decode(hashEsperado);
            byte[] b = Base64.getUrlDecoder().decode(hash(tokenInformado));
            return MessageDigest.isEqual(a, b);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
