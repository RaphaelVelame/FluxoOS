package br.com.fluxoos.application.porta;

import br.com.fluxoos.domain.seguranca.TipoEventoSeguranca;
import br.com.fluxoos.domain.seguranca.EventoSeguranca;

import java.time.Instant;
import java.util.List;

public interface RepositorioEventosSeguranca {
    EventoSeguranca registrar(TipoEventoSeguranca tipo, String detalhes, Long autorId, Long alvoId, Instant quando);
    List<EventoSeguranca> listar();
}
