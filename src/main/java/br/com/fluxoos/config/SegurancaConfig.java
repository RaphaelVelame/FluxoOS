package br.com.fluxoos.config;

import br.com.fluxoos.repository.*;
import br.com.fluxoos.service.*;
import br.com.fluxoos.seguranca.LimitadorTentativas;
import br.com.fluxoos.seguranca.PoliticaBloqueio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.Duration;

@Configuration
public class SegurancaConfig {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean LimitadorTentativas limitador() {
        return new LimitadorTentativas(new PoliticaBloqueio(5, Duration.ofMinutes(15), Duration.ofMinutes(15)),
                new PoliticaBloqueio(30, Duration.ofMinutes(15), Duration.ofMinutes(15)));
    }
    @Bean SessaoService sessaoService(SessoesEmMemoria sessoes, UsuarioRepository usuarios, AuditoriaService auditoria, Clock clock) {
        return new SessaoService(sessoes, usuarios, auditoria, clock, Duration.ofHours(8), Duration.ofMinutes(30));
    }
}
