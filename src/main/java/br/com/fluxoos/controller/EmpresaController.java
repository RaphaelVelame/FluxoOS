package br.com.fluxoos.controller;

import br.com.fluxoos.config.ProtecaoHttp;
import br.com.fluxoos.service.EmpresaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/plataforma/empresas")
@PreAuthorize("hasAnyRole('ADMIN_MASTER')")
public class EmpresaController {
    private final EmpresaService empresas;
    public EmpresaController(EmpresaService empresas) { this.empresas = empresas; }
    public record Criar(@NotBlank @Size(max=160) String nome) {}
    @GetMapping public List<EmpresaService.Resposta> listar(HttpServletRequest req) { return empresas.listar(ProtecaoHttp.usuario(req)); }
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public EmpresaService.Resposta criar(@Valid @RequestBody Criar entrada, HttpServletRequest req) { return empresas.criar(ProtecaoHttp.usuario(req), entrada.nome()); }
}
