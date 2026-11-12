package br.com.fluxoos.repository;

import br.com.fluxoos.seguranca.Sessao;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Value;
import br.com.fluxoos.service.LimiteSessoesException;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.Optional;

/** Armazenamento limitado por JVM; admissão e descarte são atômicos. */
@Repository
public class SessoesEmMemoria {
    private final Map<String, Sessao> dados = new HashMap<>();
    private final Clock clock;
    private final int limite;

    public SessoesEmMemoria(Clock clock, @Value("${fluxoos.seguranca.max-sessoes:20}") int limite) {
        this.clock = java.util.Objects.requireNonNull(clock);
        if (limite < 1) throw new IllegalArgumentException("Limite de sessões deve ser positivo.");
        this.limite = limite;
    }

    public synchronized void salvar(Sessao sessao) {
        limparExpiradas();
        if (dados.containsKey(sessao.getHashToken())) throw new IllegalStateException("Colisão de token de sessão");
        if (dados.size() >= limite) throw new LimiteSessoesException(limite);
        if (dados.putIfAbsent(sessao.getHashToken(), sessao) != null) {
            throw new IllegalStateException("Colisão de token de sessão");
        }
    }
    public synchronized List<Sessao> buscarPorUsuario(long usuarioId) {
        return listar().stream().filter(s -> s.getUsuarioId() == usuarioId).toList();
    }
    public synchronized Optional<Sessao> buscarPorHash(String hashToken) {
        limparExpiradas();
        return Optional.ofNullable(dados.get(hashToken));
    }

    public synchronized List<Sessao> listar() {
        limparExpiradas();
        return List.copyOf(dados.values());
    }

    public synchronized void remover(Sessao sessao) { dados.remove(sessao.getHashToken(), sessao); }

    public synchronized int quantidade() { limparExpiradas(); return dados.size(); }

    public synchronized void limparExpiradas() {
        var agora = clock.instant();
        dados.values().removeIf(s -> !s.estaValida(agora));
    }
}
