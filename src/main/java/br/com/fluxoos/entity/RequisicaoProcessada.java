package br.com.fluxoos.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "idempotencia")
public class RequisicaoProcessada implements Persistable<RequisicaoProcessada.Chave> {
    @EmbeddedId
    private Chave id;
    @Column(nullable = false, length = 64)
    private String conteudo;
    @Column(name = "movimento_id", nullable = false)
    private long movimentoId;

    protected RequisicaoProcessada() {}

    public RequisicaoProcessada(long usuarioId, String chave, String conteudo, long movimentoId) {
        this.id = new Chave(usuarioId, chave);
        this.conteudo = conteudo;
        this.movimentoId = movimentoId;
    }

    public String getConteudo() { return conteudo; }
    public long getMovimentoId() { return movimentoId; }
    @Override public Chave getId() { return id; }

    // A chave é definitiva: save deve inserir e deixar o banco recusar duplicatas.
    @Override @Transient public boolean isNew() { return true; }

    @Embeddable
    public static class Chave implements Serializable {
        @Column(name = "usuario_id")
        private long usuarioId;
        @Column(length = 80)
        private String chave;

        protected Chave() {}
        public Chave(long usuarioId, String chave) { this.usuarioId = usuarioId; this.chave = chave; }

        @Override public boolean equals(Object outro) {
            return outro instanceof Chave outra && usuarioId == outra.usuarioId && Objects.equals(chave, outra.chave);
        }
        @Override public int hashCode() { return Objects.hash(usuarioId, chave); }
    }
}
