package br.com.fluxoos.infrastructure.web;

import br.com.fluxoos.domain.modelo.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

final class Contratos {
    private Contratos() {}
    record Login(@NotBlank @Size(max=80) String usuario, @NotBlank @Size(max=1024) String senha) {
        @Override public String toString() { return "Login[credenciais=PROTEGIDAS]"; }
    }
    record NovaSenha(@NotBlank @Size(max=1024) String atual, @NotBlank @Size(min=15,max=1024) String nova) {
        @Override public String toString() { return "NovaSenha[credenciais=PROTEGIDAS]"; }
    }
    record AcessoView(UsuarioView usuario, String csrf, Instant expiraEm) {
        @Override public String toString() { return "AcessoView[tokens=PROTEGIDOS]"; }
    }
    record NovaOrdem(@NotBlank @Size(max=160) String solicitante, @NotBlank @Size(max=240) String local,
                     @NotBlank @Size(max=4000) String problema, @NotNull Prioridade prioridade) {}
    record Atribuicao(@Positive long tecnicoId) {}
    record Relato(@NotBlank @Size(max=8000) String relato) {}
    record Conclusao(@NotBlank @Size(max=160) String responsavel, @NotBlank @Size(max=200000) String assinatura) {
        @Override public String toString() { return "Conclusao[assinatura=PROTEGIDA]"; }
    }
    record Consumo(@Positive long materialId, @Positive int quantidade, @NotBlank @Size(max=500) String observacao) {}
    record NovoMaterial(@NotBlank @Size(max=60) String codigo, @NotBlank @Size(max=160) String nome,
                        @NotBlank @Size(max=20) String unidade, @PositiveOrZero int minimo) {}
    record Movimento(@NotNull TipoMovimento tipo, @NotNull Integer variacao, @NotBlank @Size(max=500) String observacao) {}
    record Status(@NotNull Boolean ativo) {}
    record NovoUsuario(@NotBlank @Size(max=80) String login, @NotBlank @Size(max=160) String nome,
                       @NotNull Perfil perfil, @NotBlank @Size(min=15,max=1024) String senha) {
        @Override public String toString() { return "NovoUsuario[credenciais=PROTEGIDAS]"; }
    }
    record UsuarioView(long id, String login, String nome, Perfil perfil, boolean ativo) {
        static UsuarioView de(Usuario u) { return new UsuarioView(u.getId(), u.getNomeUsuario(), u.getNomeCompleto(), u.getPerfil(), u.isAtivo()); }
    }
    record ItemView(long materialId, String nome, String unidade, int quantidade) {}
    record OrdemView(long id, String solicitante, String local, String problema, Prioridade prioridade, Situacao situacao,
                     Long tecnicoId, String relato, String responsavel, boolean assinada, Instant assinadoEm, List<ItemView> itens) {
        static OrdemView de(OrdemServico o) {
            return new OrdemView(o.getId(), o.getSolicitante(), o.getLocal(), o.getProblema(), o.getPrioridade(), o.getSituacao(),
                    o.getEquipeResponsavelId(), o.getRelatoServico(), o.getNomeResponsavel(), o.getReferenciaAssinatura() != null,
                    o.getAssinadoEm(), o.getItens().stream().map(i -> new ItemView(i.getMaterial().getId(), i.getMaterial().getNome(),
                    i.getMaterial().getUnidade(), i.getQuantidadeUsada())).toList());
        }
    }
}
