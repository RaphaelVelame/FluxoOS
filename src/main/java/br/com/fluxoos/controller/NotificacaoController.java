package br.com.fluxoos.controller;

import br.com.fluxoos.config.ProtecaoHttp;
import br.com.fluxoos.service.NotificacaoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notificacoes")
@PreAuthorize("hasAnyRole('ADMIN_EMPRESA')")
public class NotificacaoController {
    private final NotificacaoService notificacoes;
    public NotificacaoController(NotificacaoService notificacoes) { this.notificacoes = notificacoes; }
    @GetMapping public List<NotificacaoService.Resposta> listar(HttpServletRequest req) { return notificacoes.listar(ProtecaoHttp.usuario(req)); }
    @PatchMapping("/{id}/leitura") public void ler(@PathVariable long id, HttpServletRequest req) { notificacoes.ler(ProtecaoHttp.usuario(req), id); }
}
