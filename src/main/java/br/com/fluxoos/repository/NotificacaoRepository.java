package br.com.fluxoos.repository;

import br.com.fluxoos.entity.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findByEmpresaIdAndDestinatarioIdOrderByIdDesc(long empresaId, long destinatarioId);
    Optional<Notificacao> findByIdAndEmpresaIdAndDestinatarioId(long id, long empresaId, long destinatarioId);
}
