package br.com.fluxoos.service;

import br.com.fluxoos.enums.TipoEventoSeguranca;
import br.com.fluxoos.seguranca.SenhaPbkdf2;
import br.com.fluxoos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fluxoos.entity.Usuario;
import java.time.Clock;
import java.util.Objects;

/**
 * Troca protegida: exige sessão + CSRF + senha atual; substitui hash de modo
 * condicional e invalida todas as sessões. O service delimita a transação.
 */
@Service
@Transactional
public class TrocaSenhaService {
    private final SessaoService sessoes;
    private final UsuarioRepository credenciais;
    private final SenhaPbkdf2 hasher;
    private final AuditoriaService eventos;
    private final Clock clock;

    public TrocaSenhaService(SessaoService sessoes, UsuarioRepository credenciais,
                             SenhaPbkdf2 hasher, AuditoriaService eventos, Clock clock) {
        this.sessoes = Objects.requireNonNull(sessoes);
        this.credenciais = Objects.requireNonNull(credenciais);
        this.hasher = Objects.requireNonNull(hasher);
        this.eventos = Objects.requireNonNull(eventos);
        this.clock = Objects.requireNonNull(clock);
    }

    public void alterar(String tokenSessao, String tokenCsrf, char[] senhaAtual, char[] senhaNova) {
        Usuario autor = sessoes.validarAlteracao(tokenSessao, tokenCsrf);
        String antigo = credenciais.buscarHashSenha(autor.getId())
                .orElseThrow(() -> new SecurityException("Credencial indisponível"));
        if (!hasher.verificar(senhaAtual, antigo)) {
            throw new SecurityException("Não foi possível alterar a senha");
        }
        if (hasher.verificar(senhaNova, antigo)) {
            throw new IllegalArgumentException("A nova senha deve ser diferente");
        }
        String novo = hasher.gerarHash(senhaNova);
        if (credenciais.substituirHashSenha(autor.getId(), antigo, novo) != 1) {
            throw new IllegalStateException("Credencial alterada concorrentemente");
        }
        // A atualização condicional também incrementa a versão de sessão da conta.
        // A invalidação só se torna efetiva quando a transação é confirmada.
        eventos.registrarSeguranca(TipoEventoSeguranca.SENHA_ALTERADA,
                "Senha alterada e sessões revogadas", autor.getId(), autor.getId(), clock.instant());
    }
}
