package br.com.fluxoos.infrastructure.web;

import br.com.fluxoos.application.seguranca.SessaoService;
import br.com.fluxoos.domain.modelo.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

@Component
public class ProtecaoHttp extends OncePerRequestFilter {
    static final String COOKIE = "fluxoos_sessao";
    static final String ATRIBUTO_USUARIO = "fluxoos.usuario";
    private final SessaoService sessoes;
    public ProtecaoHttp(SessaoService sessoes) { this.sessoes = sessoes; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "same-origin");
        res.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'");
        if (!req.getRequestURI().startsWith("/api/")) {
            chain.doFilter(req, res);
            return;
        }
        res.setHeader("Cache-Control", "no-store");
        if (req.getContentLengthLong() > 300_000) {
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
        if (req.getRequestURI().equals("/api/auth/login") && req.getMethod().equals("POST")) {
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
        chain.doFilter(req, res);
    }

    static String token(HttpServletRequest req) {
        if (req.getCookies() != null) for (Cookie cookie : req.getCookies()) {
            if (COOKIE.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    static Usuario usuario(HttpServletRequest req) { return (Usuario) req.getAttribute(ATRIBUTO_USUARIO); }

    private void erro(HttpServletResponse res, int status, String mensagem) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"mensagem\":\"" + mensagem + "\"}");
    }
}
