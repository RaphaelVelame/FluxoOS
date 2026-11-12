package br.com.fluxoos.config;

import br.com.fluxoos.service.SessaoService;
import br.com.fluxoos.seguranca.LimitadorTentativas;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Clock;

@Component
public class ManutencaoSeguranca {
    private final SessaoService sessoes;
    private final LimitadorTentativas tentativas;
    private final Clock clock;

    public ManutencaoSeguranca(SessaoService sessoes, LimitadorTentativas tentativas, Clock clock) {
        this.sessoes = sessoes;
        this.tentativas = tentativas;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${fluxoos.seguranca.limpeza-intervalo-ms:60000}",
            initialDelayString = "${fluxoos.seguranca.limpeza-intervalo-ms:60000}")
    public void limpar() {
        tentativas.limpar(clock.instant());
        sessoes.limparSessoesInvalidas();
    }
}
