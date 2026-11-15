package br.com.fluxoos.repository;

import br.com.fluxoos.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByEmpresaIdOrderByNomeAsc(long empresaId);
    Optional<Cliente> findByIdAndEmpresaId(long id, long empresaId);
}
