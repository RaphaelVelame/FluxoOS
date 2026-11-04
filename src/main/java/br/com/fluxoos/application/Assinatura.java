package br.com.fluxoos.application;

import java.util.Base64;

/** A captura é armazenada junto à conclusão na mesma transação. */
final class Assinatura {
    private static final String PREFIXO = "data:image/png;base64,";
    private Assinatura() {}

    static void validar(String valor) {
        if (valor == null || !valor.startsWith(PREFIXO) || valor.length() > 200_000) {
            throw new IllegalArgumentException("Desenhe a assinatura do responsável (PNG, até 150 KB).");
        }
        try {
            byte[] imagem = Base64.getDecoder().decode(valor.substring(PREFIXO.length()));
            byte[] cabecalho = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
            if (imagem.length < 100) throw new IllegalArgumentException();
            for (int i = 0; i < cabecalho.length; i++) {
                if (imagem[i] != cabecalho[i]) throw new IllegalArgumentException();
            }
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(imagem);
            int largura = buffer.getInt(16);
            int altura = buffer.getInt(20);
            if (largura < 50 || altura < 30 || largura > 2048 || altura > 2048) throw new IllegalArgumentException();
            var desenho = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(imagem));
            if (desenho == null) throw new IllegalArgumentException();
            int pontos = 0;
            for (int y = 0; y < desenho.getHeight(); y++) {
                for (int x = 0; x < desenho.getWidth(); x++) {
                    int pixel = desenho.getRGB(x, y);
                    if ((pixel >>> 24) > 30 && (pixel & 0x00ffffff) < 0x00dddddd) pontos++;
                }
            }
            if (pontos < 20) throw new IllegalArgumentException();
        } catch (java.io.IOException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Assinatura inválida. Desenhe novamente.");
        }
    }
}
