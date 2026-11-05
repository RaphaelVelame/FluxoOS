package br.com.fluxoos.service;

import br.com.fluxoos.repository.OrdemServicoRepository;
import br.com.fluxoos.repository.UsuarioRepository;
import br.com.fluxoos.dto.Contratos.*;
import br.com.fluxoos.entity.*;
import br.com.fluxoos.enums.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Base64;
import java.util.List;

@Service
@Transactional
public class OrdemServicoService {
    private final OrdemServicoRepository ordens;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;

    public OrdemServicoService(OrdemServicoRepository ordens, UsuarioRepository usuarios, AuditoriaService auditoria) {
        this.ordens = ordens;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<OrdemView> listar() {
        return ordens.findAllByOrderByIdDesc().stream().map(OrdemView::de).toList();
    }

    @Transactional(readOnly = true)
    public OrdemView buscar(long id) { return OrdemView.de(carregar(id)); }

    @Transactional(readOnly = true)
    public List<AuditoriaView> historico(long id) {
        carregar(id);
        return auditoria.historico(id);
    }

    @Transactional(readOnly = true)
    public byte[] assinatura(long id) {
        String assinatura = carregar(id).getReferenciaAssinatura();
        if (assinatura == null) throw new NaoEncontradoException("Esta OS ainda não possui assinatura.");
        return Base64.getDecoder().decode(assinatura.substring("data:image/png;base64,".length()));
    }

    public OrdemServico abrir(Usuario ator, String solicitante, String local, String problema, Prioridade prioridade) {
        exigirGestor(ator);
        OrdemServico ordem = ordens.save(new OrdemServico(solicitante, local, problema, prioridade));
        auditoria.registrar(ordem.getId(), ator.getId(), "ABERTURA", "Ordem de serviço aberta.");
        return ordem;
    }

    public void atribuir(Usuario ator, long id, long tecnicoId) {
        exigirGestor(ator);
        OrdemServico ordem = carregarParaAtualizar(id);
        Usuario tecnico = usuarios.findById(tecnicoId)
                .orElseThrow(() -> new NaoEncontradoException("Técnico não encontrado."));
        ordem.atribuirEquipe(ator, tecnico);
        auditoria.registrar(id, ator.getId(), "ATRIBUICAO", "Equipe atribuída a " + tecnico.getNomeCompleto() + ".");
    }

    public void iniciar(Usuario ator, long id) {
        OrdemServico ordem = carregarAutorizada(ator, id);
        if (ordem.getEquipeResponsavelId() == null) {
            throw new IllegalStateException("Designe um técnico antes de iniciar o atendimento.");
        }
        ordem.iniciar();
        auditoria.registrar(id, ator.getId(), "INICIO", "Atendimento iniciado.");
    }

    public void relatar(Usuario ator, long id, String relato) {
        OrdemServico ordem = carregarAutorizada(ator, id);
        ordem.registrarRelato(relato);
        auditoria.registrar(id, ator.getId(), "RELATO", "Relato de atendimento atualizado.");
    }

    public void concluir(Usuario ator, long id, String responsavel, String assinatura) {
        OrdemServico ordem = carregarAutorizada(ator, id);
        Assinatura.validar(assinatura);
        ordem.concluirComAssinatura(responsavel, assinatura);
        auditoria.registrar(id, ator.getId(), "CONCLUSAO", "Atendimento concluído com assinatura de " + responsavel + ".");
    }

    private OrdemServico carregarAutorizada(Usuario ator, long id) {
        OrdemServico ordem = carregarParaAtualizar(id);
        if (!ordem.podeRegistrarUso(ator)) throw new SecurityException("Sem permissão para atender esta OS.");
        return ordem;
    }

    private OrdemServico carregar(long id) {
        return ordens.findById(id).orElseThrow(() -> new NaoEncontradoException("Ordem de serviço não encontrada."));
    }

    private OrdemServico carregarParaAtualizar(long id) {
        return ordens.buscarParaAtualizar(id).orElseThrow(() -> new NaoEncontradoException("Ordem de serviço não encontrada."));
    }

    public static void exigirGestor(Usuario ator) {
        if (ator == null || !ator.podeMovimentarEstoque()) throw new SecurityException("Acesso restrito à chefia e ao suporte de TI.");
    }
}
