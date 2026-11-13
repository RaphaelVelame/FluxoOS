package br.com.fluxoos.config;

import br.com.fluxoos.service.SessaoService;
import br.com.fluxoos.entity.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@Component
public class ProtecaoHttp extends OncePerRequestFilter {
    public static final String COOKIE = "fluxoos_sessao";
    static final String ATRIBUTO_USUARIO = "fluxoos.usuario";
    private static final int LIMITE_CORPO = 300_000;
    private final SessaoService sessoes;
    private final HttpFirewall firewall;

    public ProtecaoHttp(SessaoService sessoes, HttpFirewall firewall) {
        this.sessoes = sessoes;
        this.firewall = firewall;
    }

    @Bean
    public static HttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowSemicolon(false);
        return firewall;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        FirewalledRequest requisicao;
        try {
            requisicao = firewall.getFirewalledRequest(req);
        } catch (RequestRejectedException e) {
            erro(res, 400, "Requisição inválida.");
            return;
        }
        try {
            proteger(requisicao, firewall.getFirewalledResponse(res), chain);
        } finally {
            requisicao.reset();
        }
    }

    private void proteger(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "same-origin");
        res.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'");
        // O caminho do servlet exclui o contexto e acompanha a interpretação do servidor.
        String path = req.getServletPath();
        if (!(path.equals("/api") || path.startsWith("/api/"))) {
            chain.doFilter(req, res);
            return;
        }
        res.setHeader("Cache-Control", "no-store");
        if (req.getContentLengthLong() > LIMITE_CORPO) {
            erro(res, 413, "Requisição muito grande.");
            return;
        }
        boolean leitura = Set.of("GET", "HEAD", "OPTIONS").contains(req.getMethod());
        if (!leitura && req.getHeader("Origin") != null) {
            String origem = req.getScheme() + "://" + req.getServerName()
                    + ((req.getServerPort() == 80 || req.getServerPort() == 443) ? "" : ":" + req.getServerPort());
            if (!origem.equals(req.getHeader("Origin"))) {
                erro(res, 403, "Origem da requisição recusada.");
                return;
            }
        }
        // Content-Length pode faltar (chunked). Ler no maximo limite + 1 antes do JSON.
        byte[] corpo = req.getInputStream().readNBytes(LIMITE_CORPO + 1);
        if (corpo.length > LIMITE_CORPO) {
            erro(res, 413, "Requisição muito grande.");
            return;
        }
        req = new RequisicaoComCorpo(req, corpo);
        if (path.equals("/api/auth/login") && req.getMethod().equals("POST")) {
            chain.doFilter(req, res);
            return;
        }
        Usuario usuario;
        try { usuario = sessoes.validar(token(req)); }
        catch (SecurityException e) { erro(res, 401, "Sua sessão expirou. Entre novamente."); return; }
        if (!leitura) {
            try { sessoes.validarAlteracao(token(req), req.getHeader("X-CSRF-Token")); }
            catch (SecurityException e) { erro(res, 403, "Validação de segurança recusada. Entre novamente."); return; }
        }
        req.setAttribute(ATRIBUTO_USUARIO, usuario);
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities()));
        SecurityContextHolder.setContext(contexto);
        try { chain.doFilter(req, res); }
        finally { SecurityContextHolder.clearContext(); }
    }

    public static String token(HttpServletRequest req) {
        if (req.getCookies() != null) for (Cookie cookie : req.getCookies()) {
            if (COOKIE.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    public static Usuario usuario(HttpServletRequest req) { return (Usuario) req.getAttribute(ATRIBUTO_USUARIO); }

    private void erro(HttpServletResponse res, int status, String mensagem) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"mensagem\":\"" + mensagem + "\"}");
    }
}
