package br.com.fluxoos.infrastructure.config;

import br.com.fluxoos.application.porta.*;
import br.com.fluxoos.application.seguranca.*;
import br.com.fluxoos.domain.seguranca.PoliticaBloqueio;
import br.com.fluxoos.infrastructure.memoria.SessoesEmMemoria;
import br.com.fluxoos.infrastructure.seguranca.SenhaPbkdf2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.Duration;

@Configuration
public class SegurancaConfig {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean HasherSenha hasher() { return new SenhaPbkdf2(); }
    @Bean RepositorioSessoes sessoes() { return new SessoesEmMemoria(); }
    @Bean LimitadorTentativas limitador() {
        return new LimitadorTentativas(new PoliticaBloqueio(5, Duration.ofMinutes(15), Duration.ofMinutes(15)),
                new PoliticaBloqueio(30, Duration.ofMinutes(15), Duration.ofMinutes(15)));
    }
    @Bean SessaoService sessaoService(RepositorioSessoes sessoes, RepositorioUsuarios usuarios, RepositorioEventosSeguranca eventos, Clock clock) {
        return new SessaoService(sessoes, usuarios, eventos, clock, Duration.ofHours(8), Duration.ofMinutes(30));
    }
    @Bean AutenticacaoService autenticacao(RepositorioUsuarios usuarios, RepositorioCredenciais credenciais, HasherSenha hasher,
                                         LimitadorTentativas limite, SessaoService sessoes, RepositorioEventosSeguranca eventos, Clock clock) {
        return new AutenticacaoService(usuarios, credenciais, hasher, limite, sessoes, eventos, clock);
    }
    @Bean TrocaSenhaService trocaSenha(SessaoService sessoes, RepositorioCredenciais credenciais, HasherSenha hasher,
                                     RepositorioEventosSeguranca eventos, Clock clock) {
        return new TrocaSenhaService(sessoes, credenciais, hasher, eventos, clock);
    }
}
