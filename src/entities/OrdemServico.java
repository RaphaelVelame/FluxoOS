package entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OrdemServico {
    private final long id;
    private final String solicitante;
    private final String local;
    private final String problema;
    private final Prioridade prioridade;
    private Situacao situacao;
    private Long equipeResponsavelId; 
    private String relatoServico;
    private String nomeResponsavel;
    private String referenciaAssinatura;
    private Instant assinadoEm;
    private final List<ItemOS> itens = new ArrayList<>();
    private final List<Auditavel> historicoEventos = new ArrayList<>();

    public OrdemServico(long id, String solicitante, String local,
                        String problema, Prioridade prioridade) {
        this.id = Validacoes.idPositivo(id, "ID da OS");
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
        EventoAtribuicao evento = new EventoAtribuicao(equipeResponsavelId, tecnico, gestor);
        equipeResponsavelId = tecnico.getId();
        registrarEvento(evento);
    }
    boolean podeRegistrarUso(Usuario ator) {
        return ator != null && ator.isAtivo() &&
                (ator.getPerfil().podeMovimentarEstoque() ||
                 (ator.getPerfil() == Perfil.TECNICO && equipeResponsavelId != null
                  && equipeResponsavelId == ator.getId()));
    }

    public void registrarAuditoria(Usuario ator, String tipo, String detalhes) {
        if (!podeRegistrarUso(ator)) {
            throw new SecurityException("Sem permissão para registrar auditoria nesta OS.");
        }
        registrarEvento(new EventoAuditoria(tipo, detalhes, ator));
    }

    void validarInclusaoItem(Material material, int quantidadeUsada) {
        exigirEmAndamento();
        Objects.requireNonNull(material, "Material obrigatório.");
        Validacoes.positivo(quantidadeUsada, "Quantidade usada");
        boolean jaExiste = itens.stream()
                .anyMatch(item -> item.getMaterial().getId() == material.getId());
        if (jaExiste) {
            throw new IllegalStateException("Material já registrado nesta OS; use um fluxo de correção.");
        }
    }

    void adicionarItemValidado(ItemOS item) {
        validarInclusaoItem(item.getMaterial(), item.getQuantidadeUsada());
        itens.add(item);
    }

    void registrarEvento(Auditavel evento) {
        historicoEventos.add(Objects.requireNonNull(evento, "Evento obrigatório."));
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
    public List<Auditavel> getHistoricoEventos() { return List.copyOf(historicoEventos); }
}
