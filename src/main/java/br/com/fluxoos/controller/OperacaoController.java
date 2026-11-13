package br.com.fluxoos.controller;

import br.com.fluxoos.service.*;
import br.com.fluxoos.config.ProtecaoHttp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import br.com.fluxoos.dto.CriarOsDTO;
import br.com.fluxoos.dto.AtribuirOsDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import static br.com.fluxoos.dto.Contratos.*;

@RestController
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
public class OperacaoController {
    private final OrdemServicoService ordens;
    private final EstoqueService estoque;
    private final UsuarioService usuarios;
    private final AuditoriaService auditoria;

    public OperacaoController(OrdemServicoService ordens, EstoqueService estoque,
                              UsuarioService usuarios, AuditoriaService auditoria) {
        this.ordens = ordens; this.estoque = estoque; this.usuarios = usuarios; this.auditoria = auditoria;
    }

    @GetMapping("/materiais") public List<MaterialView> materiais(HttpServletRequest req) { return estoque.listar(ProtecaoHttp.usuario(req)); }
    @GetMapping("/estoques") public List<EstoqueView> estoques(HttpServletRequest req) { return estoque.listarEstoques(ProtecaoHttp.usuario(req)); }
    @PostMapping("/estoques") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA','ESTOQUISTA')")
    public EstoqueView cadastrarEstoque(@Valid @RequestBody NovoEstoque e, HttpServletRequest req) {
        return EstoqueView.de(estoque.cadastrarEstoque(ProtecaoHttp.usuario(req), e.codigo(), e.nome(), e.tipo()));
    }
    @PatchMapping("/materiais/estoque")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA','ESTOQUISTA')")
    public Map<String,Integer> classificar(@Valid @RequestBody ClassificacaoEstoque e, HttpServletRequest req) {
        return Map.of("classificados", estoque.classificar(ProtecaoHttp.usuario(req), e.estoqueId(), e.materialIds()));
    }
    @GetMapping("/ordens") public List<OrdemView> ordens(HttpServletRequest req) { return ordens.listar(ProtecaoHttp.usuario(req)); }
    @GetMapping("/ordens/{id}") public OrdemView ordem(@PathVariable long id, HttpServletRequest req) { return ordens.buscar(ProtecaoHttp.usuario(req), id); }
    @GetMapping("/ordens/{id}/historico")
    public List<AuditoriaView> historico(@PathVariable long id, HttpServletRequest req) {
        return ordens.historico(ProtecaoHttp.usuario(req), id);
    }
    @GetMapping(value="/ordens/{id}/assinatura", produces=MediaType.IMAGE_PNG_VALUE)
    public byte[] assinatura(@PathVariable long id, HttpServletRequest req) {
        return ordens.assinatura(ProtecaoHttp.usuario(req), id);
    }
    @GetMapping("/movimentos") public List<MovimentoView> movimentos(HttpServletRequest req) { return estoque.listarMovimentos(ProtecaoHttp.usuario(req)); }
    @GetMapping("/seguranca/eventos") public List<EventoSegurancaView> eventos(HttpServletRequest req) {
        UsuarioService.exigirChefia(ProtecaoHttp.usuario(req));
        return auditoria.listarEventos(ProtecaoHttp.usuario(req));
    }

    @PostMapping("/ordens") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA','SUPERVISOR','SUPORTE_TECNICO')")
    public OrdemView abrir(@Valid @RequestBody CriarOsDTO e, @RequestHeader("Idempotency-Key") String chave, HttpServletRequest req) {
        return ordens.abrirComChave(ProtecaoHttp.usuario(req), e.solicitante(), e.local(), e.problema(), e.prioridade(), chave);
    }
    @PostMapping("/ordens/{id}/atribuicao")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA')")
    public void atribuir(@PathVariable long id, @Valid @RequestBody AtribuirOsDTO e, HttpServletRequest req) {
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
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA','ESTOQUISTA')")
    public MaterialView cadastrar(@Valid @RequestBody NovoMaterial e, HttpServletRequest req) {
        return MaterialView.de(estoque.cadastrar(ProtecaoHttp.usuario(req), e.codigo(), e.nome(), e.unidade(), e.minimo(), e.estoqueId()));
    }
    @PostMapping("/materiais/{id}/movimentos") public Map<String, Long> movimentar(@PathVariable long id, @Valid @RequestBody Movimento e,
            @RequestHeader("Idempotency-Key") String chave, HttpServletRequest req) {
        return Map.of("movimentoId", estoque.movimentar(ProtecaoHttp.usuario(req), id, e.tipo(), e.variacao(), e.observacao(), chave));
    }
    @PatchMapping("/materiais/{id}/status") public void statusMaterial(@PathVariable long id, @Valid @RequestBody Status e, HttpServletRequest req) {
        estoque.alterarStatus(ProtecaoHttp.usuario(req), id, e.ativo());
    }
}
