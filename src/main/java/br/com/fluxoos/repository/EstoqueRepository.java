package br.com.fluxoos.repository;

import br.com.fluxoos.entity.Estoque;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {
    List<Estoque> findAllByOrderByNomeAsc();
}
