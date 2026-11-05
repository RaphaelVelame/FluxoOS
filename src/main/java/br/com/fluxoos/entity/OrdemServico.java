package br.com.fluxoos.entity;

import br.com.fluxoos.enums.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "ordens")
public class OrdemServico {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ordem_ids")
    @SequenceGenerator(name = "ordem_ids", sequenceName = "sequencia_ids", allocationSize = 1)
    private Long id;
    @Column(nullable = false, length = 160)
    private String solicitante;
    @Column(nullable = false, length = 240)
    private String local;
    @Column(nullable = false, length = 4000)
    private String problema;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridade prioridade;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Situacao situacao;
    @Column(name = "equipe_id")
    private Long equipeResponsavelId; 
    @Column(name = "relato", length = 8000)
    private String relatoServico;
    @Column(name = "responsavel", length = 160)
    private String nomeResponsavel;
    @Column(name = "assinatura", columnDefinition = "text")
    private String referenciaAssinatura;
    @Column(name = "assinado_em")
    private Instant assinadoEm;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();
    @ElementCollection
    @CollectionTable(name = "itens_os", joinColumns = @JoinColumn(name = "ordem_id"))
    private List<ItemOS> itens = new ArrayList<>();

    protected OrdemServico() {}

    public OrdemServico(String solicitante, String local,
                        String problema, Prioridade prioridade) {
        this.solicitante = Validacoes.textoObrigatorio(solicitante, "Solicitante");
        this.local = Validacoes.textoObrigatorio(local, "Local");
        this.problema = Validacoes.textoObrigatorio(problema, "Problema");
        this.prioridade = Objects.requireNonNull(prioridade, "Prioridade obrigatória.");
        this.situacao = Situacao.ABERTA;
    }

    public void iniciar() {
        if (situacao != Situacao.ABERTA) {
            throw new IllegalStateException("Somente OS aberta pode ser iniciada.");
        }
        situacao = Situacao.EM_ANDAMENTO;
    }

    public void registrarRelato(String relato) {
        exigirEmAndamento();
        this.relatoServico = Validacoes.textoObrigatorio(relato, "Relato de serviço");
    }

    public void concluirComAssinatura(String nomeResponsavel, String referenciaAssinatura) {
        exigirEmAndamento();
        if (relatoServico == null) {
            throw new IllegalStateException("É obrigatório registrar o relato antes de concluir.");
        }
        String nomeValidado = Validacoes.textoObrigatorio(nomeResponsavel, "Responsável presente");
        String referenciaValidada = Validacoes.textoObrigatorio(referenciaAssinatura, "Referência da assinatura");
        this.nomeResponsavel = nomeValidado;
        this.referenciaAssinatura = referenciaValidada;
        this.assinadoEm = Instant.now();
        this.situacao = Situacao.CONCLUIDA;
    }

    public void atribuirEquipe(Usuario gestor, Usuario tecnico) {
        if (gestor == null || !gestor.podeMovimentarEstoque()) {
            throw new SecurityException("Somente chefia ou suporte de TI pode atribuir equipe.");
        }
        if (tecnico == null || !tecnico.isAtivo() || tecnico.getPerfil() != Perfil.TECNICO) {
            throw new IllegalArgumentException("A equipe designada deve ser um técnico ativo.");
        }
        if (situacao == Situacao.CONCLUIDA) {
            throw new IllegalStateException("Não é permitido reatribuir OS concluída.");
        }
        if (equipeResponsavelId != null && equipeResponsavelId == tecnico.getId()) {
            throw new IllegalStateException("Técnico já está designado para esta OS.");
        }
        equipeResponsavelId = tecnico.getId();
    }
    public boolean podeRegistrarUso(Usuario ator) {
        return ator != null && ator.isAtivo() &&
                (ator.getPerfil().podeMovimentarEstoque() ||
                 (ator.getPerfil() == Perfil.TECNICO && equipeResponsavelId != null
                  && equipeResponsavelId == ator.getId()));
    }

    public void validarInclusaoItem(Material material, int quantidadeUsada) {
        exigirEmAndamento();
        Objects.requireNonNull(material, "Material obrigatório.");
        Validacoes.positivo(quantidadeUsada, "Quantidade usada");
        boolean jaExiste = itens.stream()
                .anyMatch(item -> item.getMaterial().getId() == material.getId());
        if (jaExiste) {
            throw new IllegalStateException("Material já registrado nesta OS; use um fluxo de correção.");
        }
    }

    public void adicionarItem(ItemOS item) {
        validarInclusaoItem(item.getMaterial(), item.getQuantidadeUsada());
        itens.add(item);
    }

    private void exigirEmAndamento() {
        if (situacao != Situacao.EM_ANDAMENTO) {
            throw new IllegalStateException("A OS precisa estar em andamento.");
        }
    }

    public long getId() { return id; }
    public String getSolicitante() { return solicitante; }
    public String getLocal() { return local; }
    public String getProblema() { return problema; }
    public Prioridade getPrioridade() { return prioridade; }
    public Situacao getSituacao() { return situacao; }
    public Long getEquipeResponsavelId() { return equipeResponsavelId; }
    public String getRelatoServico() { return relatoServico; }
    public String getNomeResponsavel() { return nomeResponsavel; }
    public String getReferenciaAssinatura() { return referenciaAssinatura; }
    public Instant getAssinadoEm() { return assinadoEm; }
    public List<ItemOS> getItens() { return List.copyOf(itens); }
}
