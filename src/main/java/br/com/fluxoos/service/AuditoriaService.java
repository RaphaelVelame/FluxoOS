package br.com.fluxoos.service;

import br.com.fluxoos.dto.Contratos.*;
import br.com.fluxoos.entity.EventoAuditoria;
import br.com.fluxoos.enums.TipoEventoSeguranca;
import br.com.fluxoos.repository.EventoAuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class AuditoriaService {
    private final EventoAuditoriaRepository eventos;
    private final Clock clock;

    public AuditoriaService(EventoAuditoriaRepository eventos, Clock clock) {
        this.eventos = eventos;
        this.clock = clock;
    }

    public void registrar(Long ordemId, long usuarioId, String tipo, String detalhes) {
        eventos.save(new EventoAuditoria(ordemId, usuarioId, null, tipo, detalhes, clock.instant()));
    }

    public void registrarSeguranca(TipoEventoSeguranca tipo, String detalhes, Long autorId, Long alvoId, Instant quando) {
        eventos.save(new EventoAuditoria(null, autorId, alvoId, tipo.name(), detalhes, quando));
    }

    @Transactional(readOnly = true)
    public List<AuditoriaView> historico(long ordemId) {
        return eventos.findByOrdemIdOrderByIdDesc(ordemId).stream().map(AuditoriaView::de).toList();
    }

    @Transactional(readOnly = true)
    public List<EventoSegurancaView> listarEventos(br.com.fluxoos.entity.Usuario ator) {
        UsuarioService.exigirChefia(ator);
        List<String> tipos = Arrays.stream(TipoEventoSeguranca.values()).map(Enum::name).toList();
        var registros = ator.temRole(br.com.fluxoos.enums.Role.ROLE_ADMIN_MASTER) ? eventos.findByTipoInOrderByIdDesc(tipos)
                : eventos.buscarSegurancaNaEmpresa(tipos, br.com.fluxoos.seguranca.EscopoEmpresa.exigir(ator));
        return registros.stream().map(EventoSegurancaView::de).toList();
    }
}
