package br.com.fluxoos.infrastructure.web;

import br.com.fluxoos.application.*;
import br.com.fluxoos.application.porta.*;
import br.com.fluxoos.domain.modelo.Material;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static br.com.fluxoos.infrastructure.web.Contratos.*;

@RestController
@RequestMapping("/api")
public class OperacaoController {
    private final RepositorioOperacional dados;
    private final OrdemServicoService ordens;
    private final EstoqueApplicationService estoque;
    private final UsuarioService usuarios;
    private final RepositorioEventosSeguranca eventos;

    public OperacaoController(RepositorioOperacional dados, OrdemServicoService ordens, EstoqueApplicationService estoque,
                              UsuarioService usuarios, RepositorioEventosSeguranca eventos) {
        this.dados = dados; this.ordens = ordens; this.estoque = estoque; this.usuarios = usuarios; this.eventos = eventos;
    }

    @GetMapping("/usuarios") public List<UsuarioView> usuarios() { return dados.listarUsuarios().stream().map(UsuarioView::de).toList(); }
    @GetMapping("/materiais") public List<Material> materiais() { return dados.listarMateriais(); }
    @GetMapping("/ordens") @Transactional(readOnly=true)
    public List<OrdemView> ordens() { return dados.listarOrdens().stream().map(OrdemView::de).toList(); }
    @GetMapping("/ordens/{id}") public OrdemView ordem(@PathVariable long id) { return OrdemView.de(dados.ordem(id, false)); }
    @GetMapping("/ordens/{id}/historico")
    public List<RepositorioOperacional.RegistroAuditoria> historico(@PathVariable long id) {
        dados.ordem(id, false);
        return dados.historico(id);
    }
    @GetMapping(value="/ordens/{id}/assinatura", produces=MediaType.IMAGE_PNG_VALUE)
    public byte[] assinatura(@PathVariable long id) {
        String assinatura = dados.ordem(id, false).getReferenciaAssinatura();
        if (assinatura == null) throw new NaoEncontradoException("Esta OS ainda não possui assinatura.");
        return Base64.getDecoder().decode(assinatura.substring("data:image/png;base64,".length()));
    }
    @GetMapping("/movimentos") public List<RepositorioOperacional.Movimento> movimentos() { return dados.listarMovimentos(); }
    @GetMapping("/seguranca/eventos") public Object eventos(HttpServletRequest req) {
        UsuarioService.exigirChefia(ProtecaoHttp.usuario(req));
        return eventos.listar();
    }

    @PostMapping("/ordens") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public OrdemView abrir(@Valid @RequestBody NovaOrdem e, HttpServletRequest req) {
        return OrdemView.de(ordens.abrir(ProtecaoHttp.usuario(req), e.solicitante(), e.local(), e.problema(), e.prioridade()));
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
    public Material cadastrar(@Valid @RequestBody NovoMaterial e, HttpServletRequest req) {
        return estoque.cadastrar(ProtecaoHttp.usuario(req), e.codigo(), e.nome(), e.unidade(), e.minimo());
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
