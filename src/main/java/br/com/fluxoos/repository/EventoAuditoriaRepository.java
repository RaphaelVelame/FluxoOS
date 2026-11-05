package br.com.fluxoos.repository;

import br.com.fluxoos.entity.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {
    List<EventoAuditoria> findByOrdemIdOrderByIdDesc(long ordemId);
    List<EventoAuditoria> findByTipoInOrderByIdDesc(Collection<String> tipos);
}
