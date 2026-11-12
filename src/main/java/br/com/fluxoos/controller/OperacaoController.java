package br.com.fluxoos.controller;

import br.com.fluxoos.service.*;
import br.com.fluxoos.config.ProtecaoHttp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static br.com.fluxoos.dto.Contratos.*;

@RestController
@RequestMapping("/api")
public class OperacaoController {
    private final OrdemServicoService ordens;
    private final EstoqueService estoque;
    private final UsuarioService usuarios;
    private final AuditoriaService auditoria;

    public OperacaoController(OrdemServicoService ordens, EstoqueService estoque,
                              UsuarioService usuarios, AuditoriaService auditoria) {
        this.ordens = ordens; this.estoque = estoque; this.usuarios = usuarios; this.auditoria = auditoria;
    }

    @GetMapping("/usuarios") public List<UsuarioView> usuarios() { return usuarios.listar(); }
    @GetMapping("/materiais") public List<MaterialView> materiais() { return estoque.listar(); }
    @GetMapping("/ordens") public List<OrdemView> ordens() { return ordens.listar(); }
    @GetMapping("/ordens/{id}") public OrdemView ordem(@PathVariable long id) { return ordens.buscar(id); }
    @GetMapping("/ordens/{id}/historico")
    public List<AuditoriaView> historico(@PathVariable long id) {
        return ordens.historico(id);
    }
    @GetMapping(value="/ordens/{id}/assinatura", produces=MediaType.IMAGE_PNG_VALUE)
    public byte[] assinatura(@PathVariable long id) {
        return ordens.assinatura(id);
    }
    @GetMapping("/movimentos") public List<MovimentoView> movimentos() { return estoque.listarMovimentos(); }
    @GetMapping("/seguranca/eventos") public List<EventoSegurancaView> eventos(HttpServletRequest req) {
        UsuarioService.exigirChefia(ProtecaoHttp.usuario(req));
        return auditoria.listarEventos();
    }

    @PostMapping("/ordens") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public OrdemView abrir(@Valid @RequestBody NovaOrdem e, @RequestHeader("Idempotency-Key") String chave, HttpServletRequest req) {
        return ordens.abrirComChave(ProtecaoHttp.usuario(req), e.solicitante(), e.local(), e.problema(), e.prioridade(), chave);
    }
    @PostMapping("/ordens/{id}/atribuicao") public void atribuir(@PathVariable long id, @Valid @RequestBody Atribuicao e, HttpServletRequest req) {
        ordens.atribuir(ProtecaoHttp.usuario(req), id, e.tecnicoId());
    }
    @PostMapping("/ordens/{id}/inicio") public void iniciar(@PathVariable long id, HttpServletRequest req) {
        ordens.iniciar(ProtecaoHttp.usuario(req), id);
    }
    @PutMapping("/ordens/{id}/relato") public void relatar(@PathVariable long id, @Valid @RequestBody Relato e, HttpServletRequest req) {
        ordens.relatar(ProtecaoHttp.usuario(req), id, e.relato());
    }
    @PostMapping("/ordens/{id}/conclusao") public void concluir(@PathVariable long id, @Valid @RequestBody Conclusao e, HttpServletRequest req) {
        ordens.concluir(ProtecaoHttp.usuario(req), id, e.responsavel(), e.assinatura());
    }
    @PostMapping("/ordens/{id}/materiais") public Map<String, Long> consumir(@PathVariable long id, @Valid @RequestBody Consumo e,
            @RequestHeader("Idempotency-Key") String chave, HttpServletRequest req) {
        return Map.of("movimentoId", estoque.consumir(ProtecaoHttp.usuario(req), id, e.materialId(), e.quantidade(), e.observacao(), chave));
    }
    @PostMapping("/materiais") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public MaterialView cadastrar(@Valid @RequestBody NovoMaterial e, HttpServletRequest req) {
        return MaterialView.de(estoque.cadastrar(ProtecaoHttp.usuario(req), e.codigo(), e.nome(), e.unidade(), e.minimo()));
    }
    @PostMapping("/materiais/{id}/movimentos") public Map<String, Long> movimentar(@PathVariable long id, @Valid @RequestBody Movimento e,
            @RequestHeader("Idempotency-Key") String chave, HttpServletRequest req) {
        return Map.of("movimentoId", estoque.movimentar(ProtecaoHttp.usuario(req), id, e.tipo(), e.variacao(), e.observacao(), chave));
    }
    @PatchMapping("/materiais/{id}/status") public void statusMaterial(@PathVariable long id, @Valid @RequestBody Status e, HttpServletRequest req) {
        estoque.alterarStatus(ProtecaoHttp.usuario(req), id, e.ativo());
    }
    @PostMapping("/usuarios") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public UsuarioView cadastrarUsuario(@Valid @RequestBody NovoUsuario e, HttpServletRequest req) {
        char[] senha = e.senha().toCharArray();
        try { return UsuarioView.de(usuarios.cadastrar(ProtecaoHttp.usuario(req), e.login(), e.nome(), e.perfil(), senha)); }
        finally { Arrays.fill(senha, '\0'); }
    }
    @PatchMapping("/usuarios/{id}/status") public void statusUsuario(@PathVariable long id, @Valid @RequestBody Status e, HttpServletRequest req) {
        usuarios.alterarStatus(ProtecaoHttp.usuario(req), id, e.ativo());
    }
}
