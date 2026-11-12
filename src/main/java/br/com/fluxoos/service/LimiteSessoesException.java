package br.com.fluxoos.service;

public final class LimiteSessoesException extends RuntimeException {
    public LimiteSessoesException(int limite) {
        super("Limite de " + limite + " sessões ativas atingido. Encerre uma sessão ou aguarde sua expiração.");
    }
}
