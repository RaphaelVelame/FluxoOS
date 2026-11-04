package br.com.fluxoos.application.seguranca;

import java.util.Locale;

/** Canonicalização deve ser aplicada identicamente ao cadastrar e autenticar. */
public final class Identificadores {
    private Identificadores() {}

    public static String normalizarUsuario(String nome) {
        if (nome == null) throw new IllegalArgumentException("Usuário inválido");
        String normalizado = nome.strip().toLowerCase(Locale.ROOT);
        if (normalizado.isEmpty() || normalizado.length() > 80) {
            throw new IllegalArgumentException("Usuário inválido");
        }
        return normalizado;
    }

    /**
     * Somente endereços literais; nunca aceite nome de host como chave de limite.
     * O endereço remoto deve ser extraído de conexão/proxy confiável na API.
     */
    public static String validarIp(String ip) {
        if (ip == null) throw new IllegalArgumentException("IP inválido");
        String valor = ip.strip().toLowerCase(Locale.ROOT);
        if (valor.length() < 2 || valor.length() > 45) throw new IllegalArgumentException("IP inválido");
        if (valor.indexOf(':') >= 0) {
            // Com dois-pontos, exige literal IPv6 (não existe consulta a hostname).
            if (!valor.matches("[0-9a-f:.]+")) throw new IllegalArgumentException("IP inválido");
            try {
                java.net.InetAddress endereco = java.net.InetAddress.getByName(valor);
                if (!(endereco instanceof java.net.Inet6Address) &&
                        !(endereco instanceof java.net.Inet4Address)) {
                    throw new IllegalArgumentException("IP inválido");
                }
                return endereco.getHostAddress().toLowerCase(Locale.ROOT);
            } catch (java.net.UnknownHostException e) {
                throw new IllegalArgumentException("IP inválido", e);
            }
        }
        String[] partes = valor.split("\\.", -1);
        if (partes.length != 4) throw new IllegalArgumentException("IP inválido");
        StringBuilder canonico = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            String parte = partes[i];
            if (parte.isEmpty() || parte.length() > 3 || !parte.matches("[0-9]+")) {
                throw new IllegalArgumentException("IP inválido");
            }
            int numero = Integer.parseInt(parte);
            if (numero > 255) throw new IllegalArgumentException("IP inválido");
            if (i > 0) canonico.append('.');
            canonico.append(numero);
        }
        return canonico.toString();
    }
}
