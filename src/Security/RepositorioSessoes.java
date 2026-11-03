package seguranca;

import java.util.Optional;

/** Interface (polimorfismo): trocar a implementação sem mudar a regra. */
public interface RepositorioSessoes {
    void salvar(Sessao sessao);
    Optional<Sessao> buscarPorHash(String hashToken);
    java.util.List<Sessao> buscarPorUsuario(long usuarioId);
}
