package br.com.fluxoos.entity;

import br.com.fluxoos.enums.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "ordens")
public class OrdemServico extends RegistroEmpresa {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ordem_ids")
    @SequenceGenerator(name = "ordem_ids", sequenceName = "sequencia_ids", allocationSize = 1)
    private Long id;
    @Column(name = "autor_abertura_id", updatable = false)
    private Long autorAberturaId;
    @Column(name = "chave_abertura", length = 80, updatable = false)
    private String chaveAbertura;
    @Column(name = "conteudo_abertura", length = 64, updatable = false)
    private String conteudoAbertura;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_atendimento", nullable = false, length = 20)
    private TipoAtendimento tipoAtendimento = TipoAtendimento.INTERNO;
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

    public OrdemServico(long empresaId, String solicitante, String local, String problema, Prioridade prioridade) {
        this(solicitante, local, problema, prioridade);
        vincularEmpresa(empresaId);
    }

    public void identificarAbertura(long autorId, String chave, String conteudo) {
        if (id != null || chaveAbertura != null) throw new IllegalStateException("Abertura já identificada.");
        autorAberturaId = autorId;
        chaveAbertura = chave;
        conteudoAbertura = conteudo;
    }

    public OrdemServico(long empresaId, String solicitante, String local, String problema, Prioridade prioridade, Cliente cliente) {
        this(empresaId, solicitante, local, problema, prioridade);
        definirCliente(cliente);
    }

    private void definirCliente(Cliente cliente) {
        if (cliente != null && !Objects.equals(getEmpresaId(), cliente.getEmpresaId()))
            throw new IllegalArgumentException("Cliente de outra empresa.");
        this.cliente = cliente;
        this.tipoAtendimento = cliente == null ? TipoAtendimento.INTERNO : TipoAtendimento.CLIENTE;
    }

    public void classificarAtendimento(Cliente cliente) {
        if (tipoAtendimento != TipoAtendimento.LEGADO || !itens.isEmpty() || situacao == Situacao.CONCLUIDA)
            throw new IllegalStateException("Somente OS legada sem consumo e não concluída pode identificar o cliente.");
        definirCliente(cliente);
    }

    public void validarOrigemMaterial(Estoque estoque) {
        if (estoque != null && !Objects.equals(getEmpresaId(), estoque.getEmpresaId()))
            throw new IllegalArgumentException("Estoque de outra empresa.");
        if (tipoAtendimento == TipoAtendimento.CLIENTE && estoque == null)
            throw new IllegalStateException("Classifique o proprietário do material antes de consumi-lo numa OS de cliente.");
        if (estoque != null && estoque.getTipo() == TipoEstoque.CLIENTE
                && (cliente == null || !Objects.equals(estoque.getClienteId(), cliente.getId())))
            throw new IllegalStateException("Material de cliente só pode ser utilizado na OS do próprio cliente.");
    }

    public Cliente getCliente() { return cliente; }
    public TipoAtendimento getTipoAtendimento() { return tipoAtendimento; }

    public String getConteudoAbertura() { return conteudoAbertura; }

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
        if (gestor == null || !gestor.temRole(Role.ROLE_ADMIN_EMPRESA) || !Objects.equals(getEmpresaId(), gestor.getEmpresaId())) {
            throw new SecurityException("Somente o administrador da empresa pode atribuir equipe.");
        }
        if (tecnico == null || !Objects.equals(getEmpresaId(), tecnico.getEmpresaId())
                || !tecnico.temAlgumaRole(Role.ROLE_SUPORTE_TECNICO, Role.ROLE_SUPORTE_EXTERNO)) {
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
        return ator != null && ator.isAtivo() && Objects.equals(getEmpresaId(), ator.getEmpresaId()) &&
                (ator.temRole(Role.ROLE_ADMIN_EMPRESA) ||
                 (ator.temAlgumaRole(Role.ROLE_SUPORTE_TECNICO, Role.ROLE_SUPORTE_EXTERNO) && equipeResponsavelId != null
                  && equipeResponsavelId == ator.getId()));
    }

    public void validarInclusaoItem(Material material, int quantidadeUsada) {
        exigirEmAndamento();
        Objects.requireNonNull(material, "Material obrigatório.");
        if (!Objects.equals(getEmpresaId(), material.getEmpresaId())) throw new IllegalArgumentException("Material de outra empresa.");
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
