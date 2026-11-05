package br.com.fluxoos.repository;

import br.com.fluxoos.entity.Usuario;
import org.springframework.data.jpa.repository.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByNomeUsuario(String nomeUsuario);
    List<Usuario> findAllByOrderByNomeCompletoAsc();

    @Query("select u.hashSenha from Usuario u where u.id = :usuarioId")
    Optional<String> buscarHashSenha(long usuarioId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.hashSenha = :novo where u.id = :usuarioId and u.hashSenha = :atual")
    int substituirHashSenha(long usuarioId, String atual, String novo);
}
