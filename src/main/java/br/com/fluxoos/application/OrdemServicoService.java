package br.com.fluxoos.application;

import br.com.fluxoos.application.porta.RepositorioOperacional;
import br.com.fluxoos.application.porta.RepositorioUsuarios;
import br.com.fluxoos.domain.modelo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrdemServicoService {
    private final RepositorioOperacional dados;
    private final RepositorioUsuarios usuarios;

    public OrdemServicoService(RepositorioOperacional dados, RepositorioUsuarios usuarios) {
        this.dados = dados;
        this.usuarios = usuarios;
    }

    public OrdemServico abrir(Usuario ator, String solicitante, String local, String problema, Prioridade prioridade) {
        exigirGestor(ator);
        OrdemServico ordem = new OrdemServico(dados.proximoId(), solicitante, local, problema, prioridade);
        dados.inserirOrdem(ordem);
        dados.auditar(ordem.getId(), ator.getId(), "ABERTURA", "Ordem de serviço aberta.");
        return ordem;
    }

    public void atribuir(Usuario ator, long id, long tecnicoId) {
        exigirGestor(ator);
        OrdemServico ordem = dados.ordem(id, true);
        Usuario tecnico = usuarios.buscarPorId(tecnicoId)
                .orElseThrow(() -> new NaoEncontradoException("Técnico não encontrado."));
        ordem.atribuirEquipe(ator, tecnico);
        dados.salvarOrdem(ordem);
        dados.auditar(id, ator.getId(), "ATRIBUICAO", "Equipe atribuída a " + tecnico.getNomeCompleto() + ".");
    }

    public void iniciar(Usuario ator, long id) {
        OrdemServico ordem = carregarAutorizada(ator, id);
        if (ordem.getEquipeResponsavelId() == null) {
            throw new IllegalStateException("Designe um técnico antes de iniciar o atendimento.");
        }
        ordem.iniciar();
        dados.salvarOrdem(ordem);
        dados.auditar(id, ator.getId(), "INICIO", "Atendimento iniciado.");
    }

    public void relatar(Usuario ator, long id, String relato) {
        OrdemServico ordem = carregarAutorizada(ator, id);
        ordem.registrarRelato(relato);
        dados.salvarOrdem(ordem);
        dados.auditar(id, ator.getId(), "RELATO", "Relato de atendimento atualizado.");
    }

    public void concluir(Usuario ator, long id, String responsavel, String assinatura) {
        OrdemServico ordem = carregarAutorizada(ator, id);
        Assinatura.validar(assinatura);
        ordem.concluirComAssinatura(responsavel, assinatura);
        dados.salvarOrdem(ordem);
        dados.auditar(id, ator.getId(), "CONCLUSAO", "Atendimento concluído com assinatura de " + responsavel + ".");
    }

    private OrdemServico carregarAutorizada(Usuario ator, long id) {
        OrdemServico ordem = dados.ordem(id, true);
        if (!ordem.podeRegistrarUso(ator)) throw new SecurityException("Sem permissão para atender esta OS.");
        return ordem;
    }

    public static void exigirGestor(Usuario ator) {
        if (ator == null || !ator.podeMovimentarEstoque()) throw new SecurityException("Acesso restrito à chefia e ao suporte de TI.");
    }
}
