package br.com.fluxoos.repository;

import br.com.fluxoos.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Empresa e where e.id = :id")
    java.util.Optional<Empresa> bloquear(long id);
}
