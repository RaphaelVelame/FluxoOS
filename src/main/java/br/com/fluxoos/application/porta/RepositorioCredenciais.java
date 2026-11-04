package br.com.fluxoos.application.porta;

import java.util.Optional;

public interface RepositorioCredenciais {
    Optional<String> buscarHashSenha(long usuarioId);
    boolean substituirHashSenha(long usuarioId, String hashAtual, String novoHash);
}
