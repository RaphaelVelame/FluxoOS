package br.com.fluxoos.repository;

import br.com.fluxoos.entity.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {
    List<EventoAuditoria> findByOrdemIdOrderByIdDesc(long ordemId);
    List<EventoAuditoria> findByTipoInOrderByIdDesc(Collection<String> tipos);
    @org.springframework.data.jpa.repository.Query("select e from EventoAuditoria e where e.tipo in :tipos and (e.usuarioId in (select u.id from Usuario u where u.empresaId = :empresaId) or e.alvoId in (select u.id from Usuario u where u.empresaId = :empresaId)) order by e.id desc")
    List<EventoAuditoria> buscarSegurancaNaEmpresa(Collection<String> tipos, long empresaId);
}
