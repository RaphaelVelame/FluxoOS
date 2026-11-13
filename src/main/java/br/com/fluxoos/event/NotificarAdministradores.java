package br.com.fluxoos.event;

import br.com.fluxoos.entity.Notificacao;
import br.com.fluxoos.enums.Role;
import br.com.fluxoos.repository.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.time.Clock;

@Component
public class NotificarAdministradores {
    private final UsuarioRepository usuarios;
    private final NotificacaoRepository notificacoes;
    private final Clock clock;
    public NotificarAdministradores(UsuarioRepository usuarios, NotificacaoRepository notificacoes, Clock clock) {
        this.usuarios = usuarios; this.notificacoes = notificacoes; this.clock = clock;
    }
    // Sincrono na mesma transacao: uma OS revertida nunca deixa uma notificacao orfa.
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void aoCriar(OrdemServicoCriadaEvent evento) {
        for (var admin : usuarios.buscarAtivosPorRole(evento.empresaId(), Role.ROLE_ADMIN_EMPRESA)) {
            notificacoes.save(new Notificacao(evento.empresaId(), evento.ordemId(), admin.getId(), clock.instant()));
        }
    }
}
