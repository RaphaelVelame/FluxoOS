package seguranca;

import java.time.Instant;
import java.util.List;

public interface RepositorioEventosSeguranca {
    EventoSeguranca registrar(TipoEventoSeguranca tipo, String detalhes, Long autorId, Long alvoId, Instant quando);
    List<EventoSeguranca> listar();
}
