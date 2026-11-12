package br.com.fluxoos.config;

import br.com.fluxoos.repository.*;
import br.com.fluxoos.service.*;
import br.com.fluxoos.seguranca.LimitadorTentativas;
import br.com.fluxoos.seguranca.PoliticaBloqueio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableScheduling
public class SegurancaConfig {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean LimitadorTentativas limitador(
            @Value("${fluxoos.seguranca.erros-usuario:5}") int errosUsuario,
            @Value("${fluxoos.seguranca.erros-ip:30}") int errosIp,
            @Value("${fluxoos.seguranca.janela-minutos:15}") long janela,
            @Value("${fluxoos.seguranca.bloqueio-minutos:5}") long bloqueio,
            @Value("${fluxoos.seguranca.max-identificadores:10000}") int capacidade) {
        return new LimitadorTentativas(new PoliticaBloqueio(errosUsuario, Duration.ofMinutes(janela), Duration.ofMinutes(bloqueio)),
                new PoliticaBloqueio(errosIp, Duration.ofMinutes(janela), Duration.ofMinutes(bloqueio)), capacidade);
    }
    @Bean SessaoService sessaoService(SessoesEmMemoria sessoes, UsuarioRepository usuarios, AuditoriaService auditoria, Clock clock) {
        return new SessaoService(sessoes, usuarios, auditoria, clock, Duration.ofHours(8), Duration.ofMinutes(30));
    }
}
