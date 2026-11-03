package seguranca;

import entities.Usuario;
import java.util.Optional;

public interface RepositorioUsuarios {
    Optional<Usuario> buscarPorNome(String nomeNormalizado);
    Optional<Usuario> buscarPorId(long id);
}
