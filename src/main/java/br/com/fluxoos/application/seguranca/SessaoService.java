package br.com.fluxoos.application.seguranca;

import br.com.fluxoos.domain.seguranca.TipoEventoSeguranca;
import br.com.fluxoos.application.porta.RepositorioSessoes;
import br.com.fluxoos.domain.seguranca.Sessao;
import br.com.fluxoos.application.porta.RepositorioUsuarios;
import br.com.fluxoos.domain.seguranca.TokenSeguro;
import br.com.fluxoos.application.porta.RepositorioEventosSeguranca;

import br.com.fluxoos.domain.modelo.Usuario;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Caso de uso: emite, valida, protege operações mutáveis e revoga sessões. */
public final class SessaoService {
    private final RepositorioSessoes sessoes;
    private final RepositorioUsuarios usuarios;
    private final RepositorioEventosSeguranca eventos;
    private final Clock clock;
    private final Duration tempoAbsoluto;
    private final Duration tempoInatividade;

    public SessaoService(RepositorioSessoes sessoes, RepositorioUsuarios usuarios,
                         RepositorioEventosSeguranca eventos, Clock clock,
                         Duration tempoAbsoluto, Duration tempoInatividade) {
        this.sessoes = Objects.requireNonNull(sessoes);
        this.usuarios = Objects.requireNonNull(usuarios);
        this.eventos = Objects.requireNonNull(eventos);
        this.clock = Objects.requireNonNull(clock);
        this.tempoAbsoluto = Objects.requireNonNull(tempoAbsoluto);
        this.tempoInatividade = Objects.requireNonNull(tempoInatividade);
        if (tempoAbsoluto.isNegative() || tempoAbsoluto.isZero()
                || tempoInatividade.isNegative() || tempoInatividade.isZero()) {
            throw new IllegalArgumentException("Duração inválida");
        }
    }

    /** Sem public: somente autenticadores do mesmo pacote emitem sessões. */
    TokensEmitidos criarParaUsuarioAutenticado(Usuario usuario) {
        if (usuario == null || !usuario.isAtivo()) throw new SecurityException("Usuário inativo");
        Instant agora = clock.instant();
        String token = TokenSeguro.novoToken();
        String csrf = TokenSeguro.novoToken();
        Sessao sessao = new Sessao(TokenSeguro.hash(token), TokenSeguro.hash(csrf),
                usuario.getId(), agora, tempoAbsoluto, tempoInatividade);
        sessoes.salvar(sessao);
        return new TokensEmitidos(token, csrf, sessao.getExpiraEm());
    }

    /** Valida existência, expiração absoluta/inatividade e status atual do usuário. */
    public Usuario validar(String tokenRecebido) {
        Sessao sessao = localizar(tokenRecebido);
        Instant agora = clock.instant();
        Usuario usuario = usuarios.buscarPorId(sessao.getUsuarioId())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new SecurityException("Sessão inválida"));
        if (!sessao.acessar(agora)) throw new SecurityException("Sessão inválida");
        return usuario;
    }

    /** Para POST/PUT/PATCH/DELETE com cookie: exigir cabeçalho CSRF além da sessão. */
    public Usuario validarAlteracao(String tokenRecebido, String tokenCsrfRecebido) {
        Sessao sessao = localizar(tokenRecebido);
        Instant agora = clock.instant();
        if (!sessao.estaValida(agora)) {
            throw new SecurityException("Sessão inválida");
        }
        if (!sessao.conferirCsrf(tokenCsrfRecebido)) {
            eventos.registrar(TipoEventoSeguranca.CSRF_INVALIDO, "Validação CSRF recusada",
                    sessao.getUsuarioId(), null, agora);
            throw new SecurityException("Sessão ou CSRF inválido");
        }
        return validar(tokenRecebido);
    }

    public void revogar(String tokenRecebido) {
        Sessao sessao = localizar(tokenRecebido);
        Instant agora = clock.instant();
        if (!sessao.isRevogada()) {
            sessao.revogar(agora);
            eventos.registrar(TipoEventoSeguranca.SESSAO_REVOGADA, "Sessão encerrada",
                    sessao.getUsuarioId(), null, agora);
        }
    }

    /** Após troca de senha, encerra todas as sessões, mesmo em outros dispositivos. */
    void revogarTodasDoUsuario(long usuarioId) {
        Instant agora = clock.instant();
        for (Sessao sessao : sessoes.buscarPorUsuario(usuarioId)) {
            sessao.revogar(agora);
        }
    }

    private Sessao localizar(String tokenRecebido) {
        String hash;
        try { hash = TokenSeguro.hash(tokenRecebido); }
        catch (IllegalArgumentException e) { throw new SecurityException("Sessão inválida"); }
        return sessoes.buscarPorHash(hash)
                .orElseThrow(() -> new SecurityException("Sessão inválida"));
    }
}
