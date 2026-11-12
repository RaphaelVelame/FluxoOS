package br.com.fluxoos.service;

import br.com.fluxoos.dto.TokensEmitidos;
import br.com.fluxoos.entity.Usuario;
import br.com.fluxoos.enums.Perfil;
import br.com.fluxoos.repository.UsuarioRepository;
import br.com.fluxoos.repository.SessoesEmMemoria;
import br.com.fluxoos.seguranca.SenhaPbkdf2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:revogacao-tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
        "spring.datasource.username=${TEST_DATABASE_USER:sa}", "spring.datasource.password=${TEST_DATABASE_PASSWORD:}",
        "fluxoos.bootstrap-password=Senha-revogacao-testes-2026!", "fluxoos.demo=false"
})
class RevogacaoSessaoTest {
    private static final String HASH = new SenhaPbkdf2().gerarHash("Senha-revogacao-testes-2026!".toCharArray());
    @Autowired UsuarioRepository usuarios;
    @Autowired UsuarioService contas;
    @Autowired SessaoService sessoes;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired TrocaSenhaService trocaSenha;
    @Autowired AuditoriaService auditoria;
    @Autowired Clock clock;
    private Usuario admin;
    private Usuario tecnico;

    @BeforeEach void preparar() {
        String sufixo = UUID.randomUUID().toString();
        admin = usuarios.save(new Usuario("admin-" + sufixo, "Chefia", Perfil.CHEFE, HASH));
        tecnico = usuarios.save(new Usuario("tecnico-" + sufixo, "Técnico", Perfil.TECNICO, HASH));
    }

    @Test void loginIniciadoAntesDaDesativacaoNaoCriaSessaoValidaDepoisDaReativacao() throws Exception {
        CountDownLatch carregouConta = new CountDownLatch(1);
        CountDownLatch emitirSessao = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var loginPendente = executor.submit(() -> {
                Usuario contaAnterior = usuarios.findById(tecnico.getId()).orElseThrow();
                carregouConta.countDown();
                if (!emitirSessao.await(10, TimeUnit.SECONDS)) throw new AssertionError("Login não liberado");
                return sessoes.criarParaUsuarioAutenticado(contaAnterior);
            });
            try {
                assertThat(carregouConta.await(10, TimeUnit.SECONDS)).isTrue();
                contas.alterarStatus(admin, tecnico.getId(), false);
                contas.alterarStatus(admin, tecnico.getId(), true);
            } finally { emitirSessao.countDown(); }
            TokensEmitidos antigo = loginPendente.get(10, TimeUnit.SECONDS);
            assertThatThrownBy(() -> sessoes.validar(antigo.tokenSessao())).isInstanceOf(SecurityException.class);
        } finally { executor.shutdownNow(); }
        TokensEmitidos novo = sessoes.criarParaUsuarioAutenticado(usuarios.findById(tecnico.getId()).orElseThrow());
        assertThat(sessoes.validar(novo.tokenSessao()).getId()).isEqualTo(tecnico.getId());
    }

    @Test void rollbackDaDesativacaoPreservaContaESessao() {
        TokensEmitidos acesso = sessoes.criarParaUsuarioAutenticado(tecnico);
        assertThatThrownBy(() -> new TransactionTemplate(transacoes).executeWithoutResult(tx -> {
            contas.alterarStatus(admin, tecnico.getId(), false);
            usuarios.flush();
            sessoes.limparSessoesInvalidas();
            throw new IllegalStateException("Falha simulada antes do commit");
        })).hasMessage("Falha simulada antes do commit");
        assertThat(usuarios.findById(tecnico.getId()).orElseThrow().isAtivo()).isTrue();
        assertThat(sessoes.validar(acesso.tokenSessao()).getId()).isEqualTo(tecnico.getId());
    }

    @Test void repetirAtivacaoNaoEncerraSessaoMasNovoCicloDeDesativacaoEncerra() {
        TokensEmitidos primeiro = sessoes.criarParaUsuarioAutenticado(tecnico);
        contas.alterarStatus(admin, tecnico.getId(), true);
        assertThat(sessoes.validar(primeiro.tokenSessao()).getId()).isEqualTo(tecnico.getId());
        contas.alterarStatus(admin, tecnico.getId(), false);
        contas.alterarStatus(admin, tecnico.getId(), true);
        TokensEmitidos segundo = sessoes.criarParaUsuarioAutenticado(usuarios.findById(tecnico.getId()).orElseThrow());
        contas.alterarStatus(admin, tecnico.getId(), false);
        contas.alterarStatus(admin, tecnico.getId(), true);
        assertThatThrownBy(() -> sessoes.validar(primeiro.tokenSessao())).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> sessoes.validarAlteracao(segundo.tokenSessao(), segundo.tokenCsrf()))
                .isInstanceOf(SecurityException.class);
    }

    @Test void versaoPersistidaInvalidaSessoesEmArmazenamentosIndependentes() {
        var outroServidor = new SessaoService(new SessoesEmMemoria(clock, 20), usuarios, auditoria, clock,
                Duration.ofHours(8), Duration.ofMinutes(30));
        TokensEmitidos primeiro = sessoes.criarParaUsuarioAutenticado(tecnico);
        TokensEmitidos segundo = outroServidor.criarParaUsuarioAutenticado(tecnico);
        contas.alterarStatus(admin, tecnico.getId(), false);
        contas.alterarStatus(admin, tecnico.getId(), true);
        assertThatThrownBy(() -> sessoes.validar(primeiro.tokenSessao())).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> outroServidor.validar(segundo.tokenSessao())).isInstanceOf(SecurityException.class);
    }

    @Test void trocaSenhaTambemImpedeSessaoEmitidaComEstadoAnteriorDaConta() {
        Usuario estadoAnterior = usuarios.findById(tecnico.getId()).orElseThrow();
        TokensEmitidos atual = sessoes.criarParaUsuarioAutenticado(tecnico);
        trocaSenha.alterar(atual.tokenSessao(), atual.tokenCsrf(), "Senha-revogacao-testes-2026!".toCharArray(),
                "Senha-substituta-testes-2026!".toCharArray());
        TokensEmitidos atrasado = sessoes.criarParaUsuarioAutenticado(estadoAnterior);
        assertThatThrownBy(() -> sessoes.validar(atrasado.tokenSessao())).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> sessoes.validar(atual.tokenSessao())).isInstanceOf(SecurityException.class);
    }

    @Test void rollbackDaTrocaSenhaPreservaSenhaESessoes() {
        TokensEmitidos atual = sessoes.criarParaUsuarioAutenticado(tecnico);
        assertThatThrownBy(() -> new TransactionTemplate(transacoes).executeWithoutResult(tx -> {
            trocaSenha.alterar(atual.tokenSessao(), atual.tokenCsrf(), "Senha-revogacao-testes-2026!".toCharArray(),
                    "Senha-substituta-testes-2026!".toCharArray());
            sessoes.limparSessoesInvalidas();
            throw new IllegalStateException("Falha simulada depois da troca de senha");
        })).hasMessage("Falha simulada depois da troca de senha");
        assertThat(usuarios.buscarHashSenha(tecnico.getId())).contains(HASH);
        assertThat(sessoes.validar(atual.tokenSessao()).getId()).isEqualTo(tecnico.getId());
    }
}
