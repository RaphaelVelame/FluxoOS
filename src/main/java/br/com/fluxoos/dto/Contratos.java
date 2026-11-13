package br.com.fluxoos.dto;

import br.com.fluxoos.entity.*;
import br.com.fluxoos.enums.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class Contratos {
    private Contratos() {}
    public record MaterialView(long id, String codigo, String nome, String unidade, int quantidade,
                               int quantidadeMinima, boolean ativo, boolean abaixoDoMinimo, String referenciaFabricante, Long estoqueId) {
        public static MaterialView de(Material m) {
            return new MaterialView(m.getId(), m.getCodigo(), m.getNome(), m.getUnidade(), m.getQuantidade(),
                    m.getQuantidadeMinima(), m.isAtivo(), m.isAbaixoDoMinimo(), m.getReferenciaFabricante(), m.getEstoqueId());
        }
    }
    public record MovimentoView(long id, long materialId, long usuarioId, Long ordemId, TipoMovimento tipo,
                                int variacao, int saldoApos, String observacao, Instant criadoEm, Long estoqueId) {
        public static MovimentoView de(MovimentoEstoque m) {
            return new MovimentoView(m.getId(), m.getMaterialId(), m.getUsuarioId(), m.getOrdemServicoId(),
                    m.getTipoMovimento(), m.getVariacao(), m.getSaldoApos(), m.getObservacao(), m.getCriadoEm(), m.getEstoqueId());
        }
    }
    public record AuditoriaView(long id, Long ordemId, Long usuarioId, String tipo, String detalhes, Instant criadoEm) {
        public static AuditoriaView de(EventoAuditoria e) {
            return new AuditoriaView(e.getId(), e.getOrdemId(), e.getUsuarioId(), e.getTipo(), e.getDetalhes(), e.getCriadoEm());
        }
    }
    public record EventoSegurancaView(long id, TipoEventoSeguranca tipoEvento, String detalhes,
                                     Long autorId, Long alvoId, Instant criadoEm) {
        public static EventoSegurancaView de(EventoAuditoria e) {
            return new EventoSegurancaView(e.getId(), TipoEventoSeguranca.valueOf(e.getTipo()), e.getDetalhes(),
                    e.getUsuarioId(), e.getAlvoId(), e.getCriadoEm());
        }
    }
    public record Login(@NotBlank @Size(max=80) String usuario, @NotBlank @Size(max=1024) String senha) {
        @Override public String toString() { return "Login[credenciais=PROTEGIDAS]"; }
    }
    public record NovaSenha(@NotBlank @Size(max=1024) String atual, @NotBlank @Size(min=15,max=1024) String nova) {
        @Override public String toString() { return "NovaSenha[credenciais=PROTEGIDAS]"; }
    }
    public record AcessoView(UsuarioView usuario, String csrf, Instant expiraEm) {
        @Override public String toString() { return "AcessoView[tokens=PROTEGIDOS]"; }
    }
    public record NovaOrdem(@NotBlank @Size(max=160) String solicitante, @NotBlank @Size(max=240) String local,
                     @NotBlank @Size(max=4000) String problema, @NotNull Prioridade prioridade) {}
    public record Atribuicao(@Positive long tecnicoId) {}
    public record Relato(@NotBlank @Size(max=8000) String relato) {}
    public record Conclusao(@NotBlank @Size(max=160) String responsavel, @NotBlank @Size(max=200000) String assinatura) {
        @Override public String toString() { return "Conclusao[assinatura=PROTEGIDA]"; }
    }
    public record Consumo(@Positive long materialId, @Positive int quantidade, @NotBlank @Size(max=500) String observacao) {}
    public record NovoMaterial(@NotBlank @Size(max=60) String codigo, @NotBlank @Size(max=160) String nome,
                        @NotBlank @Size(max=20) String unidade, @PositiveOrZero int minimo, @Positive Long estoqueId) {}
    public record NovoEstoque(@NotBlank @Size(max=60) String codigo, @NotBlank @Size(max=160) String nome,
                             @NotNull TipoEstoque tipo) {}
    public record EstoqueView(long id, String codigo, String nome, TipoEstoque tipo) {
        public static EstoqueView de(Estoque e) { return new EstoqueView(e.getId(), e.getCodigo(), e.getNome(), e.getTipo()); }
    }
    public record ClassificacaoEstoque(@NotNull @Positive Long estoqueId,
                                      @NotEmpty @Size(max=500) List<@NotNull @Positive Long> materialIds) {}
    public record Movimento(@NotNull TipoMovimento tipo, @NotNull Integer variacao, @NotBlank @Size(max=500) String observacao) {}
    public record Status(@NotNull Boolean ativo) {}
    public record NovoUsuario(@NotBlank @Size(max=80) String login, @NotBlank @Size(max=160) String nome,
                       @NotNull Perfil perfil, @NotBlank @Size(min=15,max=1024) String senha) {
        @Override public String toString() { return "NovoUsuario[credenciais=PROTEGIDAS]"; }
    }
    public record UsuarioView(long id, String login, String nome, Perfil perfil, boolean ativo) {
        public static UsuarioView de(Usuario u) { return new UsuarioView(u.getId(), u.getNomeUsuario(), u.getNomeCompleto(), u.getPerfil(), u.isAtivo()); }
    }
    public record ItemView(long materialId, String nome, String unidade, int quantidade, Long estoqueId) {}
    public record OrdemView(long id, String solicitante, String local, String problema, Prioridade prioridade, Situacao situacao,
                     Long tecnicoId, String relato, String responsavel, boolean assinada, Instant assinadoEm, List<ItemView> itens) {
        public static OrdemView de(OrdemServico o) {
            return new OrdemView(o.getId(), o.getSolicitante(), o.getLocal(), o.getProblema(), o.getPrioridade(), o.getSituacao(),
                    o.getEquipeResponsavelId(), o.getRelatoServico(), o.getNomeResponsavel(), o.getReferenciaAssinatura() != null,
                    o.getAssinadoEm(), o.getItens().stream().map(i -> new ItemView(i.getMaterial().getId(), i.getMaterial().getNome(),
                    i.getMaterial().getUnidade(), i.getQuantidadeUsada(), i.getEstoqueId())).toList());
        }
    }
}
