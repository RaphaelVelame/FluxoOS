package br.com.fluxoos.repository;

import br.com.fluxoos.entity.OrdemServico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {
    List<OrdemServico> findAllByOrderByIdDesc();
    List<OrdemServico> findByEmpresaIdOrderByIdDesc(long empresaId);
    Optional<OrdemServico> findByIdAndEmpresaId(long id, long empresaId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdemServico o where o.id = :id and o.empresaId = :empresaId")
    Optional<OrdemServico> buscarParaAtualizarNaEmpresa(long id, long empresaId);
    Optional<OrdemServico> findByAutorAberturaIdAndChaveAbertura(long autorAberturaId, String chaveAbertura);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdemServico o where o.id = :id")
    Optional<OrdemServico> buscarParaAtualizar(long id);
}
