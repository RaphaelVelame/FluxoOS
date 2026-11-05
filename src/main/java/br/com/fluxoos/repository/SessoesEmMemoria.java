package br.com.fluxoos.repository;

import br.com.fluxoos.seguranca.Sessao;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Apenas demonstração; o mapa não é compartilhado entre instâncias. */
@Repository
public class SessoesEmMemoria {
    private final Map<String, Sessao> dados = new ConcurrentHashMap<>();
    public void salvar(Sessao sessao) {
        if (dados.putIfAbsent(sessao.getHashToken(), sessao) != null) {
            throw new IllegalStateException("Colisão de token de sessão");
        }
    }
    public java.util.List<Sessao> buscarPorUsuario(long usuarioId) {
        return dados.values().stream().filter(s -> s.getUsuarioId() == usuarioId).toList();
    }
    public Optional<Sessao> buscarPorHash(String hashToken) {
        return Optional.ofNullable(dados.get(hashToken));
    }
}
