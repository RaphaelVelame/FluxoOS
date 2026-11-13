package br.com.fluxoos.service;

import br.com.fluxoos.dto.Contratos.*;
import br.com.fluxoos.entity.*;
import br.com.fluxoos.enums.TipoMovimento;
import br.com.fluxoos.enums.TipoEstoque;
import br.com.fluxoos.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import br.com.fluxoos.seguranca.EscopoEmpresa;

@Service
@Transactional
public class EstoqueService {
    private final MaterialRepository materiais;
    private final OrdemServicoRepository ordens;
    private final MovimentoEstoqueRepository movimentos;
    private final RequisicaoProcessadaRepository requisicoes;
    private final AuditoriaService auditoria;
    private final EstoqueRepository estoques;

    public EstoqueService(MaterialRepository materiais, OrdemServicoRepository ordens,
                          MovimentoEstoqueRepository movimentos, RequisicaoProcessadaRepository requisicoes,
                          AuditoriaService auditoria, EstoqueRepository estoques) {
        this.materiais = materiais;
        this.ordens = ordens;
        this.movimentos = movimentos;
        this.requisicoes = requisicoes;
        this.auditoria = auditoria;
        this.estoques = estoques;
    }

    @Transactional(readOnly = true)
    public List<MaterialView> listar(Usuario ator) {
        return materiais.findByEmpresaIdOrderByNomeAsc(EscopoEmpresa.exigir(ator)).stream().map(MaterialView::de).toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentoView> listarMovimentos(Usuario ator) {
        return movimentos.findByEmpresaIdOrderByIdDesc(EscopoEmpresa.exigir(ator)).stream().map(MovimentoView::de).toList();
    }

    public Material cadastrar(Usuario ator, String codigo, String nome, String unidade, int minimo) {
        return cadastrar(ator, codigo, nome, unidade, minimo, null);
    }

    public Material cadastrar(Usuario ator, String codigo, String nome, String unidade, int minimo, Long estoqueId) {
        OrdemServicoService.exigirGestor(ator);
        if (estoqueId != null) carregarEstoque(ator, estoqueId);
        Material material = materiais.save(new Material(EscopoEmpresa.exigir(ator), codigo, nome, unidade, minimo, estoqueId));
        auditoria.registrar(null, ator.getId(), "MATERIAL_CRIADO", "Material cadastrado: " + material.getCodigo());
        return material;
    }

    @Transactional(readOnly = true)
    public List<EstoqueView> listarEstoques(Usuario ator) {
        return estoques.findByEmpresaIdOrderByNomeAsc(EscopoEmpresa.exigir(ator)).stream().map(EstoqueView::de).toList();
    }

    public Estoque cadastrarEstoque(Usuario ator, String codigo, String nome, TipoEstoque tipo) {
        OrdemServicoService.exigirGestor(ator);
        Estoque estoque = estoques.save(new Estoque(EscopoEmpresa.exigir(ator), codigo, nome, tipo));
        auditoria.registrar(null, ator.getId(), "ESTOQUE_CRIADO", "Estoque cadastrado: " + estoque.getCodigo() + " (" + tipo + ").");
        return estoque;
    }

    public int classificar(Usuario ator, long estoqueId, List<Long> ids) {
        OrdemServicoService.exigirGestor(ator);
        Estoque destino = carregarEstoque(ator, estoqueId);
        if (ids == null || ids.isEmpty() || ids.size() > 500 || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Selecione de 1 a 500 materiais válidos.");
        }
        int classificados = 0;
        // Ordem fixa de locks; repetir a mesma classificacao e uma operacao sem efeito adicional.
        for (long id : ids.stream().distinct().sorted().toList()) {
            Material material = carregarMaterial(ator, id);
            if (material.classificarEstoque(estoqueId)) {
                auditoria.registrar(null, ator.getId(), "MATERIAL_CLASSIFICADO",
                        "Material " + material.getCodigo() + " vinculado ao estoque " + destino.getCodigo()
                                + " (materialId=" + id + ", estoqueId=" + estoqueId + "). Saldo: "
                                + material.getQuantidade() + " " + material.getUnidade() + ". Movimentos anteriores preservados.");
                classificados++;
            }
        }
        return classificados;
    }

    private Estoque carregarEstoque(Usuario ator, long id) {
        return estoques.findByIdAndEmpresaId(id, EscopoEmpresa.exigir(ator)).orElseThrow(() -> new NaoEncontradoException("Estoque não encontrado."));
    }

    public long movimentar(Usuario ator, long materialId, TipoMovimento tipo, int variacao, String observacao, String chave) {
        OrdemServicoService.exigirGestor(ator);
        Material material = carregarMaterial(ator, materialId);
        String conteudo = resumo("estoque:" + materialId + ":" + tipo + ":" + variacao + ":" + observacao);
        Long anterior = encontrarRepeticao(ator, chave, conteudo);
        if (anterior != null) return anterior;
        Objects.requireNonNull(tipo, "Tipo obrigatório.");
        if (tipo == TipoMovimento.SAIDA) throw new IllegalArgumentException("Saída de consumo deve estar vinculada à OS.");
        if (variacao == 0) throw new IllegalArgumentException("A variação não pode ser zero.");
        if ((tipo == TipoMovimento.ENTRADA || tipo == TipoMovimento.DEVOLUCAO) && variacao < 0) {
            throw new IllegalArgumentException("Entradas e devoluções devem ter variação positiva.");
        }
        MovimentoEstoque movimento = new MovimentoEstoque(material, ator, null, tipo, variacao,
                material.preverSaldo(variacao), observacao);
        material.aplicarVariacao(variacao);
        movimentos.save(movimento);
        registrarRequisicao(ator, chave, conteudo, movimento);
        auditoria.registrar(null, ator.getId(), "MOVIMENTO_ESTOQUE", movimento.descreverEvento());
        return movimento.getId();
    }

    public long consumir(Usuario ator, long ordemId, long materialId, int quantidade, String observacao, String chave) {
        // Todas as operações de consumo bloqueiam primeiro a OS e depois o material.
        OrdemServico ordem = ordens.buscarParaAtualizarNaEmpresa(ordemId, EscopoEmpresa.exigir(ator))
                .orElseThrow(() -> new NaoEncontradoException("Ordem de serviço não encontrada."));
        if (!ordem.podeRegistrarUso(ator)) throw new SecurityException("Sem permissão para registrar material nesta OS.");
        String conteudo = resumo("consumo:" + ordemId + ":" + materialId + ":" + quantidade + ":" + observacao);
        Long anterior = encontrarRepeticao(ator, chave, conteudo);
        if (anterior != null) return anterior;
        Material material = carregarMaterial(ator, materialId);
        ordem.validarInclusaoItem(material, quantidade);
        MovimentoEstoque movimento = new MovimentoEstoque(material, ator, ordem, TipoMovimento.SAIDA,
                -quantidade, material.preverSaldo(-quantidade), observacao);
        material.aplicarVariacao(-quantidade);
        ordem.adicionarItem(new ItemOS(material, quantidade));
        movimentos.save(movimento);
        registrarRequisicao(ator, chave, conteudo, movimento);
        auditoria.registrar(ordemId, ator.getId(), "CONSUMO", quantidade + " " + material.getUnidade() + " de " + material.getNome());
        return movimento.getId();
    }

    public void alterarStatus(Usuario ator, long materialId, boolean ativo) {
        OrdemServicoService.exigirGestor(ator);
        Material material = carregarMaterial(ator, materialId);
        if (ativo) material.ativar(); else material.desativar();
        auditoria.registrar(null, ator.getId(), "STATUS_MATERIAL", material.getCodigo() + (ativo ? " ativado." : " desativado."));
    }

    private Material carregarMaterial(Usuario ator, long id) {
        return materiais.buscarParaAtualizarNaEmpresa(id, EscopoEmpresa.exigir(ator)).orElseThrow(() -> new NaoEncontradoException("Material não encontrado."));
    }

    private void registrarRequisicao(Usuario ator, String chave, String conteudo, MovimentoEstoque movimento) {
        requisicoes.save(new RequisicaoProcessada(ator.getId(), chave, conteudo, movimento.getId()));
    }

    private Long encontrarRepeticao(Usuario ator, String chave, String conteudo) {
        if (chave == null || !chave.matches("[a-zA-Z0-9-]{16,80}")) {
            throw new IllegalArgumentException("Cabeçalho Idempotency-Key obrigatório (16 a 80 caracteres).");
        }
        return requisicoes.findById(new RequisicaoProcessada.Chave(ator.getId(), chave)).map(anterior -> {
            if (!anterior.getConteudo().equals(conteudo)) throw new IllegalStateException("Esta chave já foi utilizada em outra operação.");
            return anterior.getMovimentoId();
        }).orElse(null);
    }

    private static String resumo(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
