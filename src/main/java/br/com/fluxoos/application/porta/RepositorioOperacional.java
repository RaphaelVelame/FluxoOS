package br.com.fluxoos.application.porta;

import br.com.fluxoos.domain.modelo.*;
import java.util.List;
import java.util.Optional;

/** Porta transacional: os adaptadores devem bloquear agregados carregados para alteração. */
public interface RepositorioOperacional {
    long proximoId();
    List<Usuario> listarUsuarios();
    List<Material> listarMateriais();
    List<OrdemServico> listarOrdens();
    Material material(long id, boolean bloquear);
    OrdemServico ordem(long id, boolean bloquear);
    void inserirMaterial(Material material);
    void salvarMaterial(Material material);
    void inserirOrdem(OrdemServico ordem);
    void salvarOrdem(OrdemServico ordem);
    void inserirItem(long ordemId, ItemOS item);
    long inserirMovimento(MovimentoEstoque movimento);
    void auditar(Long ordemId, long usuarioId, String tipo, String detalhes);
    List<Movimento> listarMovimentos();
    List<RegistroAuditoria> historico(long ordemId);
    Optional<RequisicaoProcessada> requisicao(long usuarioId, String chave);
    void registrarRequisicao(long usuarioId, String chave, String conteudo, long movimentoId);

    record Movimento(long id, long materialId, long usuarioId, Long ordemId, String tipo,
                     int variacao, int saldoApos, String observacao, java.time.Instant criadoEm) {}
    record RegistroAuditoria(long id, Long ordemId, Long usuarioId, String tipo,
                            String detalhes, java.time.Instant criadoEm) {}
    record RequisicaoProcessada(String conteudo, long movimentoId) {}
}
