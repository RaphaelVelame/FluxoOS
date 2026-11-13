package br.com.fluxoos.controller;

import br.com.fluxoos.config.ProtecaoHttp;
import br.com.fluxoos.dto.*;
import br.com.fluxoos.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("isAuthenticated()")
public class UsuarioController {
    private final UsuarioService usuarios;
    public UsuarioController(UsuarioService usuarios) { this.usuarios = usuarios; }
    @GetMapping
    public List<UsuarioResponseDTO> listar(HttpServletRequest req) { return usuarios.listar(ProtecaoHttp.usuario(req)); }
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN_MASTER','ADMIN_EMPRESA')")
    public UsuarioResponseDTO criar(@Valid @RequestBody CriarUsuarioDTO entrada, HttpServletRequest req) {
        char[] senha = entrada.senha().toCharArray();
        try { return UsuarioResponseDTO.de(usuarios.criar(ProtecaoHttp.usuario(req), entrada.login(), entrada.nome(), entrada.roles(), senha, entrada.empresaId())); }
        finally { Arrays.fill(senha, '\0'); }
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_MASTER','ADMIN_EMPRESA')")
    public UsuarioResponseDTO editar(@PathVariable long id, @Valid @RequestBody AtualizarUsuarioDTO entrada, HttpServletRequest req) {
        return UsuarioResponseDTO.de(usuarios.editar(ProtecaoHttp.usuario(req), id, entrada));
    }
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN_MASTER','ADMIN_EMPRESA')")
    public void status(@PathVariable long id, @Valid @RequestBody br.com.fluxoos.dto.Contratos.Status entrada, HttpServletRequest req) {
        usuarios.alterarStatus(ProtecaoHttp.usuario(req), id, entrada.ativo());
    }
}
