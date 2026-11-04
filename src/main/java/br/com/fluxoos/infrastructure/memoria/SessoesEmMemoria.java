package br.com.fluxoos.infrastructure.memoria;

import br.com.fluxoos.application.porta.RepositorioSessoes;
import br.com.fluxoos.domain.seguranca.Sessao;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Apenas demonstração; o mapa não é compartilhado entre instâncias. */
public final class SessoesEmMemoria implements RepositorioSessoes {
    private final Map<String, Sessao> dados = new ConcurrentHashMap<>();
    @Override public void salvar(Sessao sessao) {
        if (dados.putIfAbsent(sessao.getHashToken(), sessao) != null) {
            throw new IllegalStateException("Colisão de token de sessão");
        }
    }
    @Override public java.util.List<Sessao> buscarPorUsuario(long usuarioId) {
        return dados.values().stream().filter(s -> s.getUsuarioId() == usuarioId).toList();
    }
    @Override public Optional<Sessao> buscarPorHash(String hashToken) {
        return Optional.ofNullable(dados.get(hashToken));
    }
}
