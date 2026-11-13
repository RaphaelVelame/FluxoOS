package br.com.fluxoos.repository;

import br.com.fluxoos.entity.MovimentoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovimentoEstoqueRepository extends JpaRepository<MovimentoEstoque, Long> {
    List<MovimentoEstoque> findAllByOrderByIdDesc();
    List<MovimentoEstoque> findByEmpresaIdOrderByIdDesc(long empresaId);
}
