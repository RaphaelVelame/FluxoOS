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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
@EnableScheduling
@EnableMethodSecurity
public class SegurancaConfig {
    @Bean org.springframework.security.core.userdetails.UserDetailsService userDetailsService(UsuarioRepository usuarios) {
        return login -> usuarios.findByNomeUsuario(login).orElseThrow(() ->
                new org.springframework.security.core.userdetails.UsernameNotFoundException("Credenciais recusadas."));
    }
    @Bean SecurityFilterChain api(HttpSecurity http, ProtecaoHttp protecao) throws Exception {
        // CSRF e sessoes sao validados pelo filtro existente; nao ha Basic/form login nem HttpSession.
        return http.csrf(c -> c.disable()).formLogin(c -> c.disable()).httpBasic(c -> c.disable())
                .logout(c -> c.disable()).requestCache(c -> c.disable())
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(c -> c.requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api", "/api/**").authenticated().anyRequest().permitAll())
                .addFilterBefore(protecao, AnonymousAuthenticationFilter.class)
                .build();
    }
    @Bean FilterRegistrationBean<ProtecaoHttp> registroFiltro(ProtecaoHttp filtro) {
        var registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false); // Executar uma unica vez, dentro da cadeia Spring Security.
        return registro;
    }
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
