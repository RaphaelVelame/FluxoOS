package br.com.fluxoos.infrastructure.persistencia;

import br.com.fluxoos.application.NaoEncontradoException;
import br.com.fluxoos.application.porta.RepositorioOperacional;
import br.com.fluxoos.domain.modelo.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class OperacionalJdbc implements RepositorioOperacional {
    private final JdbcTemplate jdbc;

    public OperacionalJdbc(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long proximoId() { return jdbc.queryForObject("SELECT nextval('sequencia_ids')", Long.class); }

    public List<Usuario> listarUsuarios() {
        return jdbc.query("SELECT * FROM usuarios ORDER BY nome_completo", UsuariosJdbc::mapear);
    }

    public List<Material> listarMateriais() {
        return jdbc.query("SELECT * FROM materiais ORDER BY nome", this::mapearMaterial);
    }

    public List<OrdemServico> listarOrdens() {
        return jdbc.query("SELECT * FROM ordens ORDER BY id DESC", this::mapearOrdem);
    }

    public Material material(long id, boolean bloquear) {
        return jdbc.query("SELECT * FROM materiais WHERE id = ?" + (bloquear ? " FOR UPDATE" : ""),
                this::mapearMaterial, id).stream().findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Material não encontrado."));
    }

    public OrdemServico ordem(long id, boolean bloquear) {
        return jdbc.query("SELECT * FROM ordens WHERE id = ?" + (bloquear ? " FOR UPDATE" : ""),
                this::mapearOrdem, id).stream().findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Ordem de serviço não encontrada."));
    }

    public void inserirMaterial(Material m) {
        jdbc.update("INSERT INTO materiais (id,codigo,nome,unidade,quantidade,quantidade_minima,ativo) VALUES (?,?,?,?,?,?,?)",
                m.getId(), m.getCodigo(), m.getNome(), m.getUnidade(), m.getQuantidade(), m.getQuantidadeMinima(), m.isAtivo());
    }

    public void salvarMaterial(Material m) {
        jdbc.update("UPDATE materiais SET quantidade=?, ativo=? WHERE id=?", m.getQuantidade(), m.isAtivo(), m.getId());
    }

    public void inserirOrdem(OrdemServico o) {
        jdbc.update("INSERT INTO ordens (id,solicitante,local,problema,prioridade,situacao) VALUES (?,?,?,?,?,?)",
                o.getId(), o.getSolicitante(), o.getLocal(), o.getProblema(), o.getPrioridade().name(), o.getSituacao().name());
    }

    public void salvarOrdem(OrdemServico o) {
        jdbc.update("UPDATE ordens SET situacao=?, equipe_id=?, relato=?, responsavel=?, assinatura=?, assinado_em=? WHERE id=?",
                o.getSituacao().name(), o.getEquipeResponsavelId(), o.getRelatoServico(), o.getNomeResponsavel(),
                o.getReferenciaAssinatura(), o.getAssinadoEm() == null ? null : Timestamp.from(o.getAssinadoEm()), o.getId());
    }

    public void inserirItem(long ordemId, ItemOS item) {
        jdbc.update("INSERT INTO itens_os (ordem_id,material_id,quantidade) VALUES (?,?,?)",
                ordemId, item.getMaterial().getId(), item.getQuantidadeUsada());
    }

    public long inserirMovimento(MovimentoEstoque m) {
        long id = proximoId();
        jdbc.update("INSERT INTO movimentos (id,material_id,usuario_id,ordem_id,tipo,variacao,saldo_apos,observacao) VALUES (?,?,?,?,?,?,?,?)",
                id, m.getMaterialId(), m.getUsuarioId(), m.getOrdemServicoId(), m.getTipoMovimento().name(),
                m.getVariacao(), m.getSaldoApos(), m.getObservacao());
        return id;
    }

    public void auditar(Long ordemId, long usuarioId, String tipo, String detalhes) {
        jdbc.update("INSERT INTO auditoria (id,ordem_id,usuario_id,tipo,detalhes) VALUES (?,?,?,?,?)",
                proximoId(), ordemId, usuarioId, tipo, detalhes);
    }

    public List<Movimento> listarMovimentos() {
        return jdbc.query("SELECT * FROM movimentos ORDER BY id DESC", (r, n) -> new Movimento(r.getLong("id"),
                r.getLong("material_id"), r.getLong("usuario_id"), r.getObject("ordem_id", Long.class), r.getString("tipo"),
                r.getInt("variacao"), r.getInt("saldo_apos"), r.getString("observacao"), r.getTimestamp("criado_em").toInstant()));
    }

    public List<RegistroAuditoria> historico(long ordemId) {
        return jdbc.query("SELECT * FROM auditoria WHERE ordem_id=? ORDER BY id DESC", (r, n) ->
                new RegistroAuditoria(r.getLong("id"), r.getObject("ordem_id", Long.class), r.getObject("usuario_id", Long.class),
                        r.getString("tipo"), r.getString("detalhes"), r.getTimestamp("criado_em").toInstant()), ordemId);
    }

    public Optional<RequisicaoProcessada> requisicao(long usuarioId, String chave) {
        return jdbc.query("SELECT conteudo,movimento_id FROM idempotencia WHERE usuario_id=? AND chave=?",
                (r, n) -> new RequisicaoProcessada(r.getString(1), r.getLong(2)), usuarioId, chave).stream().findFirst();
    }

    public void registrarRequisicao(long usuarioId, String chave, String conteudo, long movimentoId) {
        jdbc.update("INSERT INTO idempotencia (usuario_id,chave,conteudo,movimento_id) VALUES (?,?,?,?)",
                usuarioId, chave, conteudo, movimentoId);
    }

    private Material mapearMaterial(ResultSet r, int n) throws SQLException {
        return new Material(r.getLong("id"), r.getString("codigo"), r.getString("nome"), r.getString("unidade"),
                r.getInt("quantidade"), r.getInt("quantidade_minima"), r.getBoolean("ativo"));
    }

    private OrdemServico mapearOrdem(ResultSet r, int n) throws SQLException {
        long id = r.getLong("id");
        List<ItemOS> itens = jdbc.query("SELECT m.*,i.quantidade AS quantidade_usada FROM itens_os i JOIN materiais m ON m.id=i.material_id WHERE i.ordem_id=?",
                (item, index) -> new ItemOS(mapearMaterial(item, index), item.getInt("quantidade_usada")), id);
        Timestamp assinadoEm = r.getTimestamp("assinado_em");
        return OrdemServico.reconstituir(id, r.getString("solicitante"), r.getString("local"), r.getString("problema"),
                Prioridade.valueOf(r.getString("prioridade")), Situacao.valueOf(r.getString("situacao")),
                r.getObject("equipe_id", Long.class), r.getString("relato"), r.getString("responsavel"),
                r.getString("assinatura"), assinadoEm == null ? null : assinadoEm.toInstant(), itens);
    }
}
