package br.com.fluxoos.infrastructure.memoria;

import br.com.fluxoos.application.porta.RepositorioCredenciais;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Cadastro inicial didático; nenhuma API HTTP deve permitir gravar hash livremente. */
public final class CredenciaisEmMemoria implements RepositorioCredenciais {
    private final Map<Long, String> hashes = new ConcurrentHashMap<>();
    public void cadastrarHashInicial(long usuarioId, String hashSenha) {
        if (usuarioId <= 0 || hashSenha == null || !hashSenha.startsWith("pbkdf2_sha256$")) {
            throw new IllegalArgumentException("Credencial inválida");
        }
        if (hashes.putIfAbsent(usuarioId, hashSenha) != null) {
            throw new IllegalStateException("Credencial já cadastrada");
        }
    }
    @Override public boolean substituirHashSenha(long usuarioId, String hashAtual, String novoHash) {
        return hashes.replace(usuarioId, hashAtual, novoHash);
    }
    @Override public Optional<String> buscarHashSenha(long usuarioId) {
        return Optional.ofNullable(hashes.get(usuarioId));
    }
}
