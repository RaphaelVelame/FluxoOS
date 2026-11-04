package br.com.fluxoos.application;

import br.com.fluxoos.application.porta.RepositorioOperacional;
import br.com.fluxoos.domain.modelo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@Transactional
public class EstoqueApplicationService {
    private final RepositorioOperacional dados;
    public EstoqueApplicationService(RepositorioOperacional dados) { this.dados = dados; }

    public Material cadastrar(Usuario ator, String codigo, String nome, String unidade, int minimo) {
        OrdemServicoService.exigirGestor(ator);
        Material material = new Material(dados.proximoId(), codigo, nome, unidade, 0, minimo, true);
        dados.inserirMaterial(material);
        dados.auditar(null, ator.getId(), "MATERIAL_CRIADO", "Material cadastrado: " + material.getCodigo());
        return material;
    }

    public long movimentar(Usuario ator, long materialId, TipoMovimento tipo, int variacao, String observacao, String chave) {
        OrdemServicoService.exigirGestor(ator);
        Material material = dados.material(materialId, true);
        String conteudo = resumo("estoque:" + materialId + ":" + tipo + ":" + variacao + ":" + observacao);
        Long anterior = encontrarRepeticao(ator, chave, conteudo);
        if (anterior != null) return anterior;
        MovimentoEstoque movimento = new EstoqueService().registrarMovimento(ator, material, tipo, variacao, observacao);
        dados.salvarMaterial(material);
        long id = dados.inserirMovimento(movimento);
        dados.registrarRequisicao(ator.getId(), chave, conteudo, id);
        dados.auditar(null, ator.getId(), "MOVIMENTO_ESTOQUE", movimento.descreverEvento());
        return id;
    }

    public long consumir(Usuario ator, long ordemId, long materialId, int quantidade, String observacao, String chave) {
        // A ordem de bloqueio é sempre OS -> material para evitar perda de atualização.
        OrdemServico ordem = dados.ordem(ordemId, true);
        if (!ordem.podeRegistrarUso(ator)) throw new SecurityException("Sem permissão para registrar material nesta OS.");
        String conteudo = resumo("consumo:" + ordemId + ":" + materialId + ":" + quantidade + ":" + observacao);
        Long anterior = encontrarRepeticao(ator, chave, conteudo);
        if (anterior != null) return anterior;
        Material material = dados.material(materialId, true);
        MovimentoEstoque movimento = new EstoqueService().registrarUsoNaOs(ator, ordem, material, quantidade, observacao);
        dados.salvarMaterial(material);
        dados.inserirItem(ordemId, new ItemOS(material, quantidade));
        long id = dados.inserirMovimento(movimento);
        dados.registrarRequisicao(ator.getId(), chave, conteudo, id);
        dados.auditar(ordemId, ator.getId(), "CONSUMO", quantidade + " " + material.getUnidade() + " de " + material.getNome());
        return id;
    }

    public void alterarStatus(Usuario ator, long materialId, boolean ativo) {
        OrdemServicoService.exigirGestor(ator);
        Material material = dados.material(materialId, true);
        new EstoqueService().alterarStatusMaterial(ator, material, ativo);
        dados.salvarMaterial(material);
        dados.auditar(null, ator.getId(), "STATUS_MATERIAL", material.getCodigo() + (ativo ? " ativado." : " desativado."));
    }

    private Long encontrarRepeticao(Usuario ator, String chave, String conteudo) {
        if (chave == null || !chave.matches("[a-zA-Z0-9-]{16,80}")) {
            throw new IllegalArgumentException("Cabeçalho Idempotency-Key obrigatório (16 a 80 caracteres).");
        }
        return dados.requisicao(ator.getId(), chave).map(anterior -> {
            if (!anterior.conteudo().equals(conteudo)) {
                throw new IllegalStateException("Esta chave já foi utilizada em outra operação.");
            }
            return anterior.movimentoId();
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
