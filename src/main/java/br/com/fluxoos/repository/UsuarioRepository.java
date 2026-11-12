package br.com.fluxoos.repository;

import br.com.fluxoos.entity.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.transaction.annotation.Propagation;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByNomeUsuario(String nomeUsuario);
    List<Usuario> findAllByOrderByNomeCompletoAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarParaAtualizar(long id);

    // Lê somente estado confirmado, mesmo se a limpeza for chamada dentro de outra transação.
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @Query("select u.id as id, u.ativo as ativo, u.versaoSessao as versaoSessao from Usuario u where u.id in :ids")
    List<EstadoAcesso> buscarEstadosDeAcesso(Collection<Long> ids);

    interface EstadoAcesso {
        long getId();
        boolean getAtivo();
        long getVersaoSessao();
    }

    @Query("select u.hashSenha from Usuario u where u.id = :usuarioId")
    Optional<String> buscarHashSenha(long usuarioId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.hashSenha = :novo, u.versaoSessao = u.versaoSessao + 1 where u.id = :usuarioId and u.hashSenha = :atual")
    int substituirHashSenha(long usuarioId, String atual, String novo);
}
