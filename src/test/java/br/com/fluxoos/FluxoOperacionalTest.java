package br.com.fluxoos;

import br.com.fluxoos.service.*;
import br.com.fluxoos.repository.*;
import br.com.fluxoos.entity.*;
import br.com.fluxoos.enums.*;
import br.com.fluxoos.seguranca.SenhaPbkdf2;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fluxoos-tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "fluxoos.bootstrap-password=Senha-inicial-testes-2026!", "fluxoos.demo=false"
})
@AutoConfigureMockMvc
class FluxoOperacionalTest {
    private static final String SENHA = "Senha-inicial-testes-2026!";
    private static final String HASH = new SenhaPbkdf2().gerarHash(SENHA.toCharArray());
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MaterialRepository materiais;
    @Autowired OrdemServicoRepository ordensRepository;
    @Autowired MovimentoEstoqueRepository movimentos;
    @Autowired EventoAuditoriaRepository auditoria;
    @Autowired RequisicaoProcessadaRepository requisicoes;
    private final Map<Long, Usuario> contas = new HashMap<>();

    @Autowired UsuarioRepository usuarios;
    @Autowired OrdemServicoService ordens;
    @Autowired EstoqueService estoque;

    @BeforeEach void preparar() {
        requisicoes.deleteAllInBatch();
        movimentos.deleteAllInBatch();
        auditoria.deleteAllInBatch();
        ordensRepository.deleteAll();
        materiais.deleteAllInBatch();
        usuarios.deleteAllInBatch();
        contas.clear();
        inserirUsuario(1, "admin", Perfil.CHEFE);
        inserirUsuario(2, "tecnico", Perfil.TECNICO);
        inserirUsuario(3, "outro", Perfil.TECNICO);
        inserirUsuario(4, "consulta", Perfil.SOLICITANTE);
    }

    @Test void fluxoHttpCompletoComAssinaturaEHistorico() throws Exception {
        Acesso admin = login("admin");
        JsonNode criada = enviar(post("/api/ordens"), Map.of("solicitante", "Ana", "local", "Escola", "problema", "Rede indisponível", "prioridade", "ALTA"), admin, 201);
        long id = criada.get("id").asLong();
        enviar(post("/api/ordens/" + id + "/atribuicao"), Map.of("tecnicoId", usuario(2).getId()), admin, 200);
        JsonNode material = enviar(post("/api/materiais"), Map.of("codigo", "RJ45", "nome", "Conector", "unidade", "un", "minimo", 5), admin, 201);
        long materialId = material.get("id").asLong();
        var entrada = Map.of("tipo", "ENTRADA", "variacao", 20, "observacao", "Reposição");
        String chaveEntrada = UUID.randomUUID().toString();
        JsonNode movimento = enviar(post("/api/materiais/" + materialId + "/movimentos").header("Idempotency-Key", chaveEntrada), entrada, admin, 200);
        JsonNode repetido = enviar(post("/api/materiais/" + materialId + "/movimentos").header("Idempotency-Key", chaveEntrada), entrada, admin, 200);
        assertThat(repetido).isEqualTo(movimento);
        Acesso tecnico = login("tecnico");
        enviar(post("/api/ordens/" + id + "/inicio"), null, tecnico, 200);
        String chaveConsumo = UUID.randomUUID().toString();
        var consumo = Map.of("materialId", materialId, "quantidade", 2, "observacao", "Ponto da recepção");
        enviar(post("/api/ordens/" + id + "/materiais").header("Idempotency-Key", chaveConsumo), consumo, tecnico, 200);
        enviar(post("/api/ordens/" + id + "/materiais").header("Idempotency-Key", chaveConsumo), consumo, tecnico, 200);
        assertThat(materiais.findById(materialId).orElseThrow().getQuantidade()).isEqualTo(18);
        String assinatura = assinatura();
        enviar(post("/api/ordens/" + id + "/conclusao"), Map.of("responsavel", "Ana", "assinatura", assinatura), tecnico, 409);
        enviar(put("/api/ordens/" + id + "/relato"), Map.of("relato", "Conectores substituídos e conexão testada."), tecnico, 200);
        enviar(post("/api/ordens/" + id + "/conclusao"), Map.of("responsavel", "Ana", "assinatura", assinatura), tecnico, 200);
        JsonNode finalizada = enviar(get("/api/ordens/" + id), null, tecnico, 200);
        assertThat(finalizada.get("situacao").asText()).isEqualTo("CONCLUIDA");
        assertThat(finalizada.get("assinada").asBoolean()).isTrue();
        assertThat(finalizada.has("referenciaAssinatura")).isFalse();
        assertThat(finalizada.get("itens")).hasSize(1);
        mvc.perform(get("/api/ordens/" + id + "/assinatura").cookie(tecnico.cookie)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        assertThat(ordens.historico(id)).hasSize(6);
        assertThat(movimentos.findAll()).hasSize(2);
        enviar(post("/api/ordens/" + id + "/inicio"), null, tecnico, 409);
    }

    @Test void exigeSessaoCsrfEPermissoesNoServidor() throws Exception {
        mvc.perform(get("/api/materiais")).andExpect(status().isUnauthorized());
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(header().exists("Content-Security-Policy"));
        Acesso tecnico = login("tecnico");
        mvc.perform(post("/api/ordens").cookie(tecnico.cookie).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        enviar(post("/api/ordens"), Map.of("solicitante", "Ana", "local", "Escola", "problema", "Rede", "prioridade", "ALTA"), tecnico, 403);
        enviar(post("/api/usuarios"), Map.of("login", "nova", "nome", "Nova Pessoa", "perfil", "CHEFE", "senha", SENHA), tecnico, 403);
        mvc.perform(post("/api/auth/logout").cookie(tecnico.cookie).header("X-CSRF-Token", tecnico.csrf).header("Origin", "https://outro.local"))
                .andExpect(status().isForbidden());
        long id = prepararOrdem();
        enviar(post("/api/ordens/" + id + "/inicio"), null, login("outro"), 403);
        enviar(get("/api/seguranca/eventos"), null, tecnico, 403);
    }

    @Test void saldoInsuficienteNaoAlteraAgregados() {
        long id = prepararOrdem();
        Material material = prepararMaterial(3);
        assertThatThrownBy(() -> estoque.consumir(usuario(2), id, material.getId(), 4, "Uso", chave()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("insuficiente");
        assertThat(materiais.findById(material.getId()).orElseThrow().getQuantidade()).isEqualTo(3);
        assertThat(ordens.buscar(id).itens()).isEmpty();
        assertThat(movimentos.findAll()).isEmpty();
    }

    @Test void falhaNaPersistenciaReverteSaldoEItemNaMesmaTransacao() {
        long id = prepararOrdem();
        Material material = prepararMaterial(10);
        assertThatThrownBy(() -> estoque.consumir(usuario(2), id, material.getId(), 2, "x".repeat(501), chave()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(materiais.findById(material.getId()).orElseThrow().getQuantidade()).isEqualTo(10);
        assertThat(ordens.buscar(id).itens()).isEmpty();
        assertThat(movimentos.findAll()).isEmpty();
    }

    @Test void consumoConcorrenteNaoPermiteSaldoNegativo() throws Exception {
        long primeira = prepararOrdem();
        long segunda = prepararOrdem();
        Material material = prepararMaterial(10);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (long id : List.of(primeira, segunda)) resultados.add(pool.submit(() -> {
                inicio.await();
                try { estoque.consumir(usuario(2), id, material.getId(), 7, "Uso concorrente", chave()); return true; }
                catch (IllegalStateException e) { return false; }
            }));
            inicio.countDown();
            int sucessos = 0;
            for (Future<Boolean> resultado : resultados) if (resultado.get(10, TimeUnit.SECONDS)) sucessos++;
            assertThat(sucessos).isEqualTo(1);
            assertThat(materiais.findById(material.getId()).orElseThrow().getQuantidade()).isEqualTo(3);
            assertThat(movimentos.findAll()).hasSize(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void rejeitaChaveReutilizadaComOutroConteudoEConsumoDuplicado() {
        long id = prepararOrdem();
        Material material = prepararMaterial(10);
        String chave = chave();
        estoque.consumir(usuario(2), id, material.getId(), 2, "Uso", chave);
        assertThatThrownBy(() -> estoque.consumir(usuario(2), id, material.getId(), 3, "Uso", chave))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("chave");
        assertThatThrownBy(() -> estoque.consumir(usuario(2), id, material.getId(), 2, "Uso", chave()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("já registrado");
        assertThat(materiais.findById(material.getId()).orElseThrow().getQuantidade()).isEqualTo(8);
    }

    @Test void persistenciaNaoSobrescreveChaveDeIdempotenciaJaConfirmada() {
        Material material = prepararMaterial(10);
        Usuario admin = usuario(1);
        String chave = chave();
        long movimentoId = estoque.movimentar(admin, material.getId(), TipoMovimento.ENTRADA, 5, "Reposição", chave);
        var id = new RequisicaoProcessada.Chave(admin.getId(), chave);
        String conteudo = requisicoes.findById(id).orElseThrow().getConteudo();
        assertThatThrownBy(() -> requisicoes.save(new RequisicaoProcessada(admin.getId(), chave, "outro", movimentoId)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(requisicoes.findById(id).orElseThrow().getConteudo()).isEqualTo(conteudo);
        assertThat(estoque.movimentar(admin, material.getId(), TipoMovimento.ENTRADA, 5, "Reposição", chave)).isEqualTo(movimentoId);
        assertThat(materiais.findById(material.getId()).orElseThrow().getQuantidade()).isEqualTo(15);
    }

    @Test void tecnicoNaoMovimentaEstoqueManualNemMaterialInativo() {
        Material material = prepararMaterial(10);
        assertThatThrownBy(() -> estoque.movimentar(usuario(2), material.getId(), TipoMovimento.ENTRADA, 5, "Uso", chave()))
                .isInstanceOf(SecurityException.class);
        estoque.alterarStatus(usuario(1), material.getId(), false);
        long id = prepararOrdem();
        assertThatThrownBy(() -> estoque.consumir(usuario(2), id, material.getId(), 2, "Uso", chave()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("inativo");
    }

    @Test void assinaturaAusenteOuEmBrancoNaoConclui() throws Exception {
        long id = prepararOrdem();
        ordens.relatar(usuario(2), id, "Atendimento realizado.");
        assertThatThrownBy(() -> ordens.concluir(usuario(2), id, "Ana", "arquivo-inexistente"))
                .isInstanceOf(IllegalArgumentException.class);
        BufferedImage branco = new BufferedImage(300, 180, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(branco, "png", bytes);
        assertThatThrownBy(() -> ordens.concluir(usuario(2), id, "Ana", "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(ordens.buscar(id).situacao()).isEqualTo(Situacao.EM_ANDAMENTO);
    }

    @Test void trocaSenhaRevogaSessoesEContaDesativadaPerdeAcesso() throws Exception {
        Acesso primeiro = login("tecnico");
        Acesso segundo = login("tecnico");
        enviar(post("/api/auth/senha"), Map.of("atual", SENHA, "nova", "Minha-nova-senha-longa-2026!"), primeiro, 200);
        mvc.perform(get("/api/auth/me").cookie(segundo.cookie)).andExpect(status().isUnauthorized());
        Acesso outro = login("outro");
        enviar(patch("/api/usuarios/" + usuario(3).getId() + "/status"), Map.of("ativo", false), login("admin"), 200);
        mvc.perform(get("/api/auth/me").cookie(outro.cookie)).andExpect(status().isUnauthorized());
    }

    @Test void validaEntradasENaoExibeDadosSecretos() throws Exception {
        Acesso admin = login("admin");
        enviar(post("/api/materiais"), Map.of("codigo", "", "nome", "", "unidade", "", "minimo", -1), admin, 400);
        JsonNode lista = enviar(get("/api/usuarios"), null, admin, 200);
        assertThat(lista.toString()).doesNotContain("hash_senha", "pbkdf2", SENHA);
        enviar(patch("/api/usuarios/" + usuario(1).getId() + "/status"), Map.of("ativo", false), admin, 400);
        enviar(get("/api/ordens/999999"), null, admin, 404);
    }

    @Test void loginFalhoTemMensagemUniformeEBloqueiaTentativas() throws Exception {
        for (int i = 0; i < 6; i++) {
            JsonNode erro = enviar(post("/api/auth/login"), Map.of("usuario", "inexistente", "senha", "senha-errada"), null, 401);
            assertThat(erro.get("mensagem").asText()).contains("Não foi possível autenticar");
        }
        assertThat(auditoria.findByTipoInOrderByIdDesc(List.of("LOGIN_BLOQUEADO")).size()).isPositive();
    }

    private void inserirUsuario(long id, String login, Perfil perfil) {
        contas.put(id, usuarios.save(new Usuario(login, login, perfil, HASH)));
    }
    private Usuario usuario(long id) { return usuarios.findById(contas.get(id).getId()).orElseThrow(); }
    private long prepararOrdem() {
        OrdemServico o = ordens.abrir(usuario(1), "Ana", "Escola", "Rede", Prioridade.ALTA);
        ordens.atribuir(usuario(1), o.getId(), usuario(2).getId());
        ordens.iniciar(usuario(2), o.getId());
        return o.getId();
    }
    private Material prepararMaterial(int quantidade) {
        Material m = new Material(chave(), "Conector", "un", quantidade, 5, true);
        materiais.save(m);
        return m;
    }
    private static String chave() { return UUID.randomUUID().toString(); }
    private Acesso login(String nome) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("usuario", nome, "senha", SENHA)))).andExpect(status().isOk()).andReturn();
        String cabecalho = resultado.getResponse().getHeader("Set-Cookie");
        assertThat(cabecalho).contains("HttpOnly", "SameSite=Strict");
        String valor = cabecalho.split(";", 2)[0].split("=", 2)[1];
        return new Acesso(new Cookie("fluxoos_sessao", valor), json.readTree(resultado.getResponse().getContentAsString()).get("csrf").asText());
    }
    private JsonNode enviar(MockHttpServletRequestBuilder requisicao, Object corpo, Acesso acesso, int statusEsperado) throws Exception {
        if (acesso != null) requisicao.cookie(acesso.cookie).header("X-CSRF-Token", acesso.csrf);
        if (corpo != null) requisicao.contentType("application/json").content(json.writeValueAsString(corpo));
        String resposta = mvc.perform(requisicao).andExpect(status().is(statusEsperado)).andReturn().getResponse().getContentAsString();
        return resposta.isBlank() ? json.nullNode() : json.readTree(resposta);
    }
    private static String assinatura() throws Exception {
        BufferedImage imagem = new BufferedImage(300, 180, BufferedImage.TYPE_INT_ARGB);
        var g = imagem.createGraphics();
        g.setColor(java.awt.Color.BLACK);
        g.setStroke(new java.awt.BasicStroke(3));
        g.drawLine(30, 100, 240, 40);
        g.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(imagem, "png", bytes);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
    private record Acesso(Cookie cookie, String csrf) {}
}
