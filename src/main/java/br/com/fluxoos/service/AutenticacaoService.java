package br.com.fluxoos.service;

import br.com.fluxoos.enums.TipoEventoSeguranca;
import br.com.fluxoos.repository.UsuarioRepository;
import br.com.fluxoos.seguranca.*;
import br.com.fluxoos.dto.TokensEmitidos;
import org.springframework.stereotype.Service;

import br.com.fluxoos.entity.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * synchronized fecha a corrida entre verificação e contagem nesta JVM;
 * em cluster precisa de limite distribuído atômico + fila/limite de concorrência.
 */
@Service
public final class AutenticacaoService {
    private final UsuarioRepository usuarios;
    private final SenhaPbkdf2 hasher;
    private final LimitadorTentativas limite;
    private final SessaoService sessoes;
    private final AuditoriaService eventos;
    private final Clock clock;
    private final String hashFicticio;

    public AutenticacaoService(UsuarioRepository usuarios, SenhaPbkdf2 hasher, LimitadorTentativas limite,
                               SessaoService sessoes, AuditoriaService eventos, Clock clock) {
        this.usuarios = Objects.requireNonNull(usuarios);
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
            eventos.registrarSeguranca(TipoEventoSeguranca.LOGIN_BLOQUEADO,
                    "Limite de autenticação excedido", null, null, agora);
            throw new AutenticacaoNegadaException();
        }
        Usuario usuario = usuarios.findByNomeUsuario(nome).orElse(null);
        String hash = usuario == null ? hashFicticio
                : usuarios.buscarHashSenha(usuario.getId()).orElse(hashFicticio);
        // Mesmo usuário inexistente consome PBKDF2: reduz diferença de tempo observável.
        boolean senhaCorreta = hasher.verificar(senha, hash);
        if (usuario == null || !usuario.isAtivo() || !senhaCorreta
                || usuarios.buscarHashSenha(usuario.getId()).isEmpty()) {
            limite.falha(nome, ip, agora);
            eventos.registrarSeguranca(TipoEventoSeguranca.LOGIN_FALHA,
                    "Credenciais recusadas", null, null, agora);
            throw new AutenticacaoNegadaException();
        }
        limite.sucesso(nome); // não zera contador agregado de IP
        TokensEmitidos emitidos = sessoes.criarParaUsuarioAutenticado(usuario);
        eventos.registrarSeguranca(TipoEventoSeguranca.LOGIN_SUCESSO,
                "Autenticação concluída", usuario.getId(), usuario.getId(), agora);
        return emitidos;
    }
}
