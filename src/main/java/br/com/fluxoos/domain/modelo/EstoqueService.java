package br.com.fluxoos.domain.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public final class EstoqueService {
    private final List<MovimentoEstoque> movimentos = new ArrayList<>();

 
    public synchronized MovimentoEstoque registrarUsoNaOs(Usuario ator, OrdemServico ordem,
                                                           Material material, int quantidade,
                                                           String observacao) {
        Objects.requireNonNull(ordem, "OS obrigatória.");
        if (!ordem.podeRegistrarUso(ator)) {
            throw new SecurityException("Sem permissão para registrar uso nesta OS.");
        }
        Objects.requireNonNull(material, "Material obrigatório.");
        ordem.validarInclusaoItem(material, quantidade);
        int delta = -quantidade; 
        int saldoPrevisto = material.preverSaldo(delta);
        ItemOS item = new ItemOS(material, quantidade);
        MovimentoEstoque movimento = new MovimentoEstoque(material, ator, ordem,
                TipoMovimento.SAIDA, delta, saldoPrevisto, observacao);

        material.aplicarVariacao(delta);
        ordem.adicionarItemValidado(item);
        movimentos.add(movimento);
        ordem.registrarEvento(movimento);
        return movimento;
    }

    public synchronized MovimentoEstoque registrarMovimento(Usuario ator, Material material,
                                                             TipoMovimento tipo, int variacao,
                                                             String observacao) {
        exigirPermissao(ator);
        Objects.requireNonNull(material, "Material obrigatório.");
        Objects.requireNonNull(tipo, "Tipo obrigatório.");
        if (tipo == TipoMovimento.SAIDA) {
            throw new IllegalArgumentException("Saída de consumo deve estar vinculada à OS.");
        }
        if (variacao == 0) {
            throw new IllegalArgumentException("A variação não pode ser zero.");
        }
        if ((tipo == TipoMovimento.ENTRADA || tipo == TipoMovimento.DEVOLUCAO) && variacao < 0) {
            throw new IllegalArgumentException("Entradas e devoluções devem ter variação positiva.");
        }
        int saldoPrevisto = material.preverSaldo(variacao);
        MovimentoEstoque movimento = new MovimentoEstoque(material, ator, null,
                tipo, variacao, saldoPrevisto, observacao);
        material.aplicarVariacao(variacao);
        movimentos.add(movimento);
        return movimento;
    }

    public synchronized void alterarStatusMaterial(Usuario ator, Material material, boolean ativo) {
        exigirPermissao(ator);
        Objects.requireNonNull(material, "Material obrigatório.");
        if (ativo) material.ativar();
        else material.desativar();
    }

    public synchronized List<MovimentoEstoque> consultarMovimentos() {
        return List.copyOf(movimentos);
    }

    private void exigirPermissao(Usuario ator) {
        if (ator == null || !ator.podeMovimentarEstoque()) {
            throw new SecurityException("Usuário sem permissão para alterar estoque.");
        }
    }
}
