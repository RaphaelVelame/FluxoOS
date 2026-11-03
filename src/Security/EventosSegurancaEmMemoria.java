package seguranca;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class EventosSegurancaEmMemoria implements RepositorioEventosSeguranca {
    private final List<EventoSeguranca> eventos = new ArrayList<>();
    private final AtomicLong sequencia = new AtomicLong();
    @Override public synchronized EventoSeguranca registrar(TipoEventoSeguranca tipo, String detalhes,
                                                                Long autorId, Long alvoId, Instant quando) {
        EventoSeguranca evento = new EventoSeguranca(sequencia.incrementAndGet(),
                tipo, detalhes, autorId, alvoId, quando);
        eventos.add(evento);
        return evento;
    }
    @Override public synchronized List<EventoSeguranca> listar() { return List.copyOf(eventos); }
}
