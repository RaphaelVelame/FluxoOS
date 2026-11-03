package entities;

final class Validacoes {
    private Validacoes() { }

    static String textoObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
        return valor.trim();
    }

    static long idPositivo(long id, String campo) {
        if (id <= 0) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero.");
        }
        return id;
    }

    static int positivo(int valor, String campo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero.");
        }
        return valor;
    }

    static int naoNegativo(int valor, String campo) {
        if (valor < 0) {
            throw new IllegalArgumentException(campo + " não pode ser negativo.");
        }
        return valor;
    }
}
