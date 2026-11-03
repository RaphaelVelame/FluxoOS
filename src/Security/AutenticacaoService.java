package seguranca;

import entities.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Login didático. synchronized fecha corrida entre verificação e contagem NESTA JVM;
 * em cluster precisa de limite distribuído atômico + fila/limite de concorrência.
 */
public final class AutenticacaoService {
    private final RepositorioUsuarios usuarios;
    private final RepositorioCredenciais credenciais;
    private final HasherSenha hasher;
    private final LimitadorTentativas limite;
    private final SessaoService sessoes;
    private final RepositorioEventosSeguranca eventos;
    private final Clock clock;
    private final String hashFicticio;

    public AutenticacaoService(RepositorioUsuarios usuarios, RepositorioCredenciais credenciais,
                               HasherSenha hasher, LimitadorTentativas limite,
                               SessaoService sessoes, RepositorioEventosSeguranca eventos, Clock clock) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.credenciais = Objects.requireNonNull(credenciais);
        this.hasher = Objects.requireNonNull(hasher);
        this.limite = Objects.requireNonNull(limite);
        this.sessoes = Objects.requireNonNull(sessoes);
        this.eventos = Objects.requireNonNull(eventos);
        this.clock = Objects.requireNonNull(clock);
        this.hashFicticio = hasher.gerarHash("senha-ficticia-nao-utilizavel".toCharArray());
    }

    public synchronized TokensEmitidos autenticar(String nomeUsuario, char[] senha, String ipOrigem) {
        String nome = Identificadores.normalizarUsuario(nomeUsuario);
        String ip = Identificadores.validarIp(ipOrigem);
        Instant agora = clock.instant();
        if (limite.bloqueado(nome, ip, agora)) {
            eventos.registrar(TipoEventoSeguranca.LOGIN_BLOQUEADO,
                    "Limite de autenticação excedido", null, null, agora);
            throw new AutenticacaoNegadaException();
        }
        Usuario usuario = usuarios.buscarPorNome(nome).orElse(null);
        String hash = usuario == null ? hashFicticio
                : credenciais.buscarHashSenha(usuario.getId()).orElse(hashFicticio);
        // Mesmo usuário inexistente consome PBKDF2: reduz diferença de tempo observável.
        boolean senhaCorreta = hasher.verificar(senha, hash);
        if (usuario == null || !usuario.isAtivo() || !senhaCorreta
                || credenciais.buscarHashSenha(usuario.getId()).isEmpty()) {
            limite.falha(nome, ip, agora);
            eventos.registrar(TipoEventoSeguranca.LOGIN_FALHA,
                    "Credenciais recusadas", null, null, agora);
            throw new AutenticacaoNegadaException();
        }
        limite.sucesso(nome); // não zera contador agregado de IP
        TokensEmitidos emitidos = sessoes.criarParaUsuarioAutenticado(usuario);
        eventos.registrar(TipoEventoSeguranca.LOGIN_SUCESSO,
                "Autenticação concluída", usuario.getId(), usuario.getId(), agora);
        return emitidos;
    }
}
