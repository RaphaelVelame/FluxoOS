package br.com.fluxoos.application.porta;

import br.com.fluxoos.domain.modelo.Usuario;
import java.util.Optional;

public interface RepositorioUsuarios {
    Optional<Usuario> buscarPorNome(String nomeNormalizado);
    Optional<Usuario> buscarPorId(long id);
}
