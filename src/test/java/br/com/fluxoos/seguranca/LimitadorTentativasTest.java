package br.com.fluxoos.seguranca;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class LimitadorTentativasTest {
    private final Instant agora = Instant.parse("2026-11-11T12:00:00Z");
    private final PoliticaBloqueio usuario = new PoliticaBloqueio(5, Duration.ofMinutes(15), Duration.ofMinutes(5));
    private final PoliticaBloqueio ip = new PoliticaBloqueio(30, Duration.ofMinutes(15), Duration.ofMinutes(5));
    private final LimitadorTentativas limite = new LimitadorTentativas(usuario, ip, 32);

    @Test void bloqueioSegueUsuarioNormalizadoMesmoTrocandoIpENaoProrroga() {
        for (int i = 0; i < 5; i++) limite.falha("Pessoa", "127.0.0.1", agora);
        assertThat(limite.bloqueado(" pessoa ", "127.0.0.2", agora)).isTrue();
        assertThat(limite.bloqueado("pessoa", "127.0.0.2", agora.plusSeconds(299))).isTrue();
        assertThat(limite.bloqueado("pessoa", "127.0.0.2", agora.plusSeconds(300))).isFalse();
    }

    @Test void sucessoDescartaUsuarioSemZerarContadorCompartilhadoDoIp() {
        limite.falha("pessoa", "127.0.0.1", agora);
        limite.sucesso("PESSOA");
        assertThat(limite.quantidadeNomes()).isZero();
        assertThat(limite.quantidadeIps()).isEqualTo(1);
        for (int i = 0; i < 29; i++) limite.falha("u" + i, "127.0.0.1", agora);
        assertThat(limite.bloqueado("nova", "127.0.0.1", agora)).isTrue();
        assertThat(limite.bloqueado("nova", "127.0.0.2", agora)).isFalse();
    }

    @Test void expiracaoRemoveEntradasSemEsperarNovaTentativaDaMesmaIdentidade() {
        limite.falha("pessoa", "127.0.0.1", agora);
        limite.limpar(agora.plus(Duration.ofMinutes(15)));
        assertThat(limite.quantidadeNomes()).isZero();
        assertThat(limite.quantidadeIps()).isZero();
    }

    @Test void janelaDeObservacaoNaoEncerraBloqueioMaisLongo() {
        var politica = new PoliticaBloqueio(5, Duration.ofMinutes(1), Duration.ofMinutes(5));
        var curto = new LimitadorTentativas(politica, ip);
        for (int i = 0; i < 5; i++) curto.falha("pessoa", "127.0.0.1", agora);
        curto.limpar(agora.plus(Duration.ofMinutes(2)));
        assertThat(curto.bloqueado("pessoa", "127.0.0.2", agora.plus(Duration.ofMinutes(2)))).isTrue();
        assertThat(curto.bloqueado("pessoa", "127.0.0.2", agora.plus(Duration.ofMinutes(5)))).isFalse();
    }

    @Test void saturacaoNaoExpulsaBloqueiosAtivosENaoUltrapassaCapacidade() {
        var pequeno = new LimitadorTentativas(usuario, ip, 3);
        for (int n = 0; n < 3; n++) for (int i = 0; i < 5; i++) pequeno.falha("u" + n, "127.0.0." + (n + 1), agora);
        for (int i = 0; i < 1000; i++) {
            assertThat(pequeno.bloqueado("novo" + i, "10.0.0.1", agora)).isTrue();
            pequeno.falha("novo" + i, "10.0.0.1", agora);
        }
        assertThat(pequeno.quantidadeNomes()).isEqualTo(3);
        assertThat(pequeno.quantidadeIps()).isEqualTo(3);
        assertThat(pequeno.bloqueado("u0", "127.0.0.1", agora)).isTrue();
        pequeno.limpar(agora.plus(Duration.ofMinutes(15)));
        assertThat(pequeno.quantidadeNomes()).isZero();
        assertThat(pequeno.quantidadeIps()).isZero();
        assertThat(pequeno.bloqueado("novo", "10.0.0.1", agora.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test void consultarIdentidadesNovasNaoPreencheMapas() {
        for (int i = 0; i < 1000; i++) {
            limite.consultarNome("novo" + i);
            limite.consultarIp("10.0.0.1");
            assertThat(limite.bloqueado("novo" + i, "10.0.0.1", agora)).isFalse();
        }
        assertThat(limite.quantidadeNomes()).isZero();
        assertThat(limite.quantidadeIps()).isZero();
    }

    @Test void falhasConcorrentesMantemContadoresLimitados() throws Exception {
        var pool = Executors.newFixedThreadPool(8);
        var inicio = new CountDownLatch(1);
        var resultados = new ArrayList<Future<?>>();
        try {
            for (int i = 0; i < 64; i++) resultados.add(pool.submit(() -> {
                inicio.await(); limite.falha("pessoa", "127.0.0.1", agora); return null;
            }));
            inicio.countDown();
            for (var r : resultados) r.get(10, TimeUnit.SECONDS);
            assertThat(limite.consultarNome("pessoa").getFalhas()).isEqualTo(5);
            assertThat(limite.consultarIp("127.0.0.1").getFalhas()).isEqualTo(30);
            assertThat(limite.quantidadeNomes()).isEqualTo(1);
            assertThat(limite.quantidadeIps()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
}
