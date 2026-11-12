package br.com.fluxoos.repository;

import br.com.fluxoos.RelogioTeste;
import br.com.fluxoos.seguranca.Sessao;
import br.com.fluxoos.seguranca.TokenSeguro;
import br.com.fluxoos.service.LimiteSessoesException;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class SessoesEmMemoriaTest {
    private final RelogioTeste clock = new RelogioTeste();
    private final SessoesEmMemoria sessoes = new SessoesEmMemoria(clock, 20);

    private Sessao nova() {
        return nova(1);
    }
    private Sessao nova(long usuarioId) {
        return new Sessao(TokenSeguro.hash(TokenSeguro.novoToken()), TokenSeguro.hash(TokenSeguro.novoToken()),
                usuarioId, 0, clock.instant(), Duration.ofHours(8), Duration.ofMinutes(30));
    }

    @Test void limiteNaoExpulsaSessoesValidas() {
        var existentes = new ArrayList<Sessao>();
        for (int i = 0; i < 20; i++) { var s = nova(i + 1); existentes.add(s); sessoes.salvar(s); }
        assertThatThrownBy(() -> sessoes.salvar(nova())).isInstanceOf(LimiteSessoesException.class);
        assertThat(sessoes.quantidade()).isEqualTo(20);
        for (var s : existentes) assertThat(sessoes.buscarPorHash(s.getHashToken())).contains(s);
    }

    @Test void admissaoConcorrenteNuncaUltrapassaVinte() throws Exception {
        var pool = Executors.newFixedThreadPool(8);
        var inicio = new CountDownLatch(1);
        var resultados = new ArrayList<Future<Boolean>>();
        try {
            for (int i = 0; i < 64; i++) resultados.add(pool.submit(() -> {
                inicio.await();
                try { sessoes.salvar(nova()); return true; }
                catch (LimiteSessoesException e) { return false; }
            }));
            inicio.countDown();
            int aceitas = 0;
            for (var resultado : resultados) if (resultado.get(10, TimeUnit.SECONDS)) aceitas++;
            assertThat(aceitas).isEqualTo(20);
            assertThat(sessoes.quantidade()).isEqualTo(20);
        } finally { pool.shutdownNow(); }
    }

    @Test void inatividadeLiberaVagasEAtividadePreservaSomenteSessaoUtilizada() {
        var ativa = nova(); sessoes.salvar(ativa);
        for (int i = 1; i < 20; i++) sessoes.salvar(nova());
        clock.avancar(Duration.ofMinutes(29));
        assertThat(ativa.acessar(clock.instant())).isTrue();
        clock.avancar(Duration.ofMinutes(1));
        sessoes.limparExpiradas();
        assertThat(sessoes.quantidade()).isEqualTo(1);
        assertThat(sessoes.buscarPorHash(ativa.getHashToken())).contains(ativa);
        sessoes.salvar(nova());
        assertThat(sessoes.quantidade()).isEqualTo(2);
    }

    @Test void usoContinuoNaoEstendeLimiteAbsoluto() {
        var sessao = nova(); sessoes.salvar(sessao);
        for (int i = 0; i < 47; i++) {
            clock.avancar(Duration.ofMinutes(10));
            assertThat(sessao.acessar(clock.instant())).isTrue();
        }
        clock.avancar(Duration.ofMinutes(10));
        assertThat(sessoes.buscarPorHash(sessao.getHashToken())).isEmpty();
        assertThat(sessoes.quantidade()).isZero();
    }

    @Test void revogadasSaoDescartadasECopiaDaListagemNaoPermiteAlterarMapa() {
        var sessao = nova(); sessoes.salvar(sessao);
        assertThatThrownBy(() -> sessoes.listar().clear()).isInstanceOf(UnsupportedOperationException.class);
        sessao.revogar(clock.instant());
        sessoes.limparExpiradas();
        assertThat(sessoes.buscarPorHash(sessao.getHashToken())).isEmpty();
        assertThat(sessoes.quantidade()).isZero();
    }
}
