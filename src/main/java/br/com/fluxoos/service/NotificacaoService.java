package br.com.fluxoos.service;

import br.com.fluxoos.entity.Usuario;
import br.com.fluxoos.repository.NotificacaoRepository;
import br.com.fluxoos.seguranca.EscopoEmpresa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class NotificacaoService {
    private final NotificacaoRepository notificacoes;
    public NotificacaoService(NotificacaoRepository notificacoes) { this.notificacoes = notificacoes; }
    public record Resposta(long id, long ordemId, String mensagem, Instant criadoEm, boolean lida) {}
    @Transactional(readOnly = true)
    public List<Resposta> listar(Usuario ator) {
        return notificacoes.findByEmpresaIdAndDestinatarioIdOrderByIdDesc(EscopoEmpresa.exigir(ator), ator.getId())
                .stream().map(n -> new Resposta(n.getId(), n.getOrdemId(), n.getMensagem(), n.getCriadoEm(), n.isLida())).toList();
    }
    public void ler(Usuario ator, long id) {
        notificacoes.findByIdAndEmpresaIdAndDestinatarioId(id, EscopoEmpresa.exigir(ator), ator.getId())
                .orElseThrow(() -> new NaoEncontradoException("Notificação não encontrada.")).marcarLida();
    }
}
