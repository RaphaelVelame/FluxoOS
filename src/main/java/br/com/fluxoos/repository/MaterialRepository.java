package br.com.fluxoos.repository;

import br.com.fluxoos.entity.Material;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findAllByOrderByNomeAsc();
    List<Material> findByEmpresaIdOrderByNomeAsc(long empresaId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Material m where m.id = :id and m.empresaId = :empresaId")
    Optional<Material> buscarParaAtualizarNaEmpresa(long id, long empresaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Material m where m.id = :id")
    Optional<Material> buscarParaAtualizar(long id);
}
