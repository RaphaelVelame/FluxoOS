package br.com.fluxoos.controller;

import br.com.fluxoos.config.ProtecaoHttp;
import br.com.fluxoos.service.*;
import br.com.fluxoos.dto.TokensEmitidos;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import static br.com.fluxoos.dto.Contratos.*;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {
    private final AutenticacaoService autenticacao;
    private final SessaoService sessoes;
    private final TrocaSenhaService trocaSenha;
    private final boolean cookieSecure;

    public AutenticacaoController(AutenticacaoService autenticacao, SessaoService sessoes, TrocaSenhaService trocaSenha,
                                  @Value("${fluxoos.cookie-secure}") boolean cookieSecure) {
        this.autenticacao = autenticacao; this.sessoes = sessoes; this.trocaSenha = trocaSenha; this.cookieSecure = cookieSecure;
    }

    @PostMapping("/login")
    public AcessoView login(@Valid @RequestBody Login entrada, HttpServletRequest req, HttpServletResponse res) {
        char[] senha = entrada.senha().toCharArray();
        TokensEmitidos tokens;
        try { tokens = autenticacao.autenticar(entrada.usuario(), senha, req.getRemoteAddr()); }
        finally { Arrays.fill(senha, '\0'); }
        res.addHeader(HttpHeaders.SET_COOKIE, cookie(tokens.tokenSessao(), 8 * 60 * 60).toString());
        return new AcessoView(UsuarioView.de(sessoes.validar(tokens.tokenSessao())), tokens.tokenCsrf(), tokens.expiraEm());
    }

    @GetMapping("/me") public UsuarioView atual(HttpServletRequest req) { return UsuarioView.de(ProtecaoHttp.usuario(req)); }

    @PostMapping("/logout")
    public void logout(HttpServletRequest req, HttpServletResponse res) {
        sessoes.revogar(ProtecaoHttp.token(req));
        res.addHeader(HttpHeaders.SET_COOKIE, cookie("", 0).toString());
    }

    @PostMapping("/senha")
    public void alterarSenha(@Valid @RequestBody NovaSenha entrada, HttpServletRequest req, HttpServletResponse res) {
        char[] atual = entrada.atual().toCharArray();
        char[] nova = entrada.nova().toCharArray();
        try { trocaSenha.alterar(ProtecaoHttp.token(req), req.getHeader("X-CSRF-Token"), atual, nova); }
        finally { Arrays.fill(atual, '\0'); Arrays.fill(nova, '\0'); }
        res.addHeader(HttpHeaders.SET_COOKIE, cookie("", 0).toString());
    }

    private ResponseCookie cookie(String token, long duracao) {
        return ResponseCookie.from(ProtecaoHttp.COOKIE, token).httpOnly(true).secure(cookieSecure).sameSite("Strict")
                .path("/api").maxAge(duracao).build();
    }
}
