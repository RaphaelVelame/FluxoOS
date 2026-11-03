package seguranca;

import entities.Usuario;
import java.time.Clock;
import java.util.Objects;

/**
 * Troca protegida: exige sessão + CSRF + senha atual; substitui hash de modo
 * condicional e invalida todas as sessões. Chamadas HTTP devem ser transacionais.
 */
public final class TrocaSenhaService {
    private final SessaoService sessoes;
    private final RepositorioCredenciais credenciais;
    private final HasherSenha hasher;
    private final RepositorioEventosSeguranca eventos;
    private final Clock clock;

    public TrocaSenhaService(SessaoService sessoes, RepositorioCredenciais credenciais,
                             HasherSenha hasher, RepositorioEventosSeguranca eventos, Clock clock) {
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
        if (!credenciais.substituirHashSenha(autor.getId(), antigo, novo)) {
            throw new IllegalStateException("Credencial alterada concorrentemente");
        }
        sessoes.revogarTodasDoUsuario(autor.getId());
        eventos.registrar(TipoEventoSeguranca.SENHA_ALTERADA,
                "Senha alterada e sessões revogadas", autor.getId(), autor.getId(), clock.instant());
    }
}
