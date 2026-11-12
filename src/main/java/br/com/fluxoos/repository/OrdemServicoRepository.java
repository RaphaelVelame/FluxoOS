package br.com.fluxoos.repository;

import br.com.fluxoos.entity.OrdemServico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {
    List<OrdemServico> findAllByOrderByIdDesc();
    Optional<OrdemServico> findByAutorAberturaIdAndChaveAbertura(long autorAberturaId, String chaveAbertura);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdemServico o where o.id = :id")
    Optional<OrdemServico> buscarParaAtualizar(long id);
}
