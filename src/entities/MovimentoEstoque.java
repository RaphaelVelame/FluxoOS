package entities;

import java.util.Objects;

public final class MovimentoEstoque extends RegistroBase {
    private final long materialId;
    private final long usuarioId;
    private final Long ordemServicoId;
    private final TipoMovimento tipoMovimento;
    private final int variacao;
    private final int saldoApos;
    private final String observacao;

    MovimentoEstoque(Material material, Usuario usuario, OrdemServico ordem,
                     TipoMovimento tipoMovimento, int variacao, int saldoApos, String observacao) {
        super();
        this.materialId = Objects.requireNonNull(material, "Material obrigatório.").getId();
        this.usuarioId = Objects.requireNonNull(usuario, "Autor obrigatório.").getId();
        this.ordemServicoId = ordem == null ? null : ordem.getId();
        this.tipoMovimento = Objects.requireNonNull(tipoMovimento, "Tipo obrigatório.");
        this.variacao = variacao;
        this.saldoApos = Validacoes.naoNegativo(saldoApos, "Saldo após movimento");
        this.observacao = Validacoes.textoObrigatorio(observacao, "Observação");
    }

    @Override
    public String descreverEvento() {
        return "Movimentação " + tipoMovimento + " material=" + materialId
                + " variacao=" + variacao + " saldoApos=" + saldoApos
                + " usuario=" + usuarioId + " OS=" + ordemServicoId;
    }

    public long getMaterialId() { return materialId; }
    public long getUsuarioId() { return usuarioId; }
    public Long getOrdemServicoId() { return ordemServicoId; }
    public TipoMovimento getTipoMovimento() { return tipoMovimento; }
    public int getVariacao() { return variacao; }
    public int getSaldoApos() { return saldoApos; }
    public String getObservacao() { return observacao; }
}
