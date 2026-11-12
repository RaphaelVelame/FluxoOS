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
        "spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:fluxoos-tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000}",
        "spring.datasource.username=${TEST_DATABASE_USER:sa}", "spring.datasource.password=${TEST_DATABASE_PASSWORD:}",
        "fluxoos.bootstrap-password=Senha-inicial-testes-2026!", "fluxoos.demo=false", "fluxoos.cookie-secure=false"
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
    @Autowired org.springframework.transaction.PlatformTransactionManager transacoes;

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
        executar(get("/api/ordens/" + id + "/assinatura").cookie(tecnico.cookie)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        assertThat(ordens.historico(id)).hasSize(6);
        assertThat(movimentos.findAll()).hasSize(2);
        enviar(post("/api/ordens/" + id + "/inicio"), null, tecnico, 409);
    }

    @Test void exigeSessaoCsrfEPermissoesNoServidor() throws Exception {
        executar(get("/api/materiais")).andExpect(status().isUnauthorized());
        executar(get("/")).andExpect(status().isOk()).andExpect(header().exists("Content-Security-Policy"));
        Acesso tecnico = login("tecnico");
        executar(post("/api/ordens").cookie(tecnico.cookie).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        enviar(post("/api/ordens"), Map.of("solicitante", "Ana", "local", "Escola", "problema", "Rede", "prioridade", "ALTA"), tecnico, 403);
        enviar(post("/api/usuarios"), Map.of("login", "nova", "nome", "Nova Pessoa", "perfil", "CHEFE", "senha", SENHA), tecnico, 403);
        executar(post("/api/auth/logout").cookie(tecnico.cookie).header("X-CSRF-Token", tecnico.csrf).header("Origin", "https://outro.local"))
                .andExpect(status().isForbidden());
        long id = prepararOrdem();
        enviar(post("/api/ordens/" + id + "/inicio"), null, login("outro"), 403);
        enviar(get("/api/seguranca/eventos"), null, tecnico, 403);
    }

    @Test void aberturaHttpIdempotenteNaoDuplicaOrdemNemAuditoria() throws Exception {
        Acesso admin = login("admin");
        String chave = chave();
        var corpo = Map.of("solicitante", "Ana", "local", "Escola", "problema", "Rede", "prioridade", "ALTA");
        JsonNode primeira = enviar(post("/api/ordens").header("Idempotency-Key", chave), corpo, admin, 201);
        JsonNode repetida = enviar(post("/api/ordens").header("Idempotency-Key", chave), corpo, admin, 201);
        assertThat(repetida.get("id")).isEqualTo(primeira.get("id"));
        assertThat(ordensRepository.count()).isEqualTo(1);
        assertThat(ordens.historico(primeira.get("id").asLong())).hasSize(1);
        enviar(post("/api/ordens").header("Idempotency-Key", chave),
                Map.of("solicitante", "Ana", "local", "Outro local", "problema", "Rede", "prioridade", "ALTA"), admin, 409);
        assertThat(ordensRepository.count()).isEqualTo(1);
    }

    @Test void aberturaConcorrenteComMesmaChaveRetornaMesmaOrdem() throws Exception {
        String chave = chave();
        Usuario admin = usuario(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            List<Future<Long>> resultados = new ArrayList<>();
            for (int i = 0; i < 2; i++) resultados.add(pool.submit(() -> {
                inicio.await();
                return ordens.abrirComChave(admin, "Ana", "Escola", "Rede", Prioridade.ALTA, chave).id();
            }));
            inicio.countDown();
            assertThat(resultados.get(0).get(10, TimeUnit.SECONDS)).isEqualTo(resultados.get(1).get(10, TimeUnit.SECONDS));
            assertThat(ordensRepository.count()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void rollbackDaAberturaPermiteRetentarComMesmaChave() {
        Usuario admin = usuario(1);
        String chave = chave();
        assertThatThrownBy(() -> new org.springframework.transaction.support.TransactionTemplate(transacoes).executeWithoutResult(tx -> {
            ordens.abrirComChave(admin, "Ana", "Escola", "Rede", Prioridade.ALTA, chave);
            throw new IllegalStateException("Falha simulada antes do commit");
        })).hasMessage("Falha simulada antes do commit");
        assertThat(ordensRepository.count()).isZero();
        long id = ordens.abrirComChave(admin, "Ana", "Escola", "Rede", Prioridade.ALTA, chave).id();
        assertThat(ordensRepository.count()).isEqualTo(1);
        assertThat(ordens.historico(id)).hasSize(1);
    }

    @Test void chaveDeAberturaPertenceAoAutor() {
        inserirUsuario(5, "suporte", Perfil.SUPORTE_TI);
        String chave = chave();
        long primeira = ordens.abrirComChave(usuario(1), "Ana", "Escola", "Rede", Prioridade.ALTA, chave).id();
        long segunda = ordens.abrirComChave(usuario(5), "Ana", "Escola", "Rede", Prioridade.ALTA, chave).id();
        assertThat(primeira).isNotEqualTo(segunda);
        assertThat(ordensRepository.count()).isEqualTo(2);
    }

    @Test void aberturaExigeChaveValidaNoHttp() throws Exception {
        Acesso admin = login("admin");
        String corpo = json.writeValueAsString(Map.of("solicitante", "Ana", "local", "Escola", "problema", "Rede", "prioridade", "ALTA"));
        mvc.perform(post("/api/ordens").servletPath("/api/ordens").cookie(admin.cookie)
                .header("X-CSRF-Token", admin.csrf).contentType("application/json").content(corpo)).andExpect(status().isBadRequest());
        enviar(post("/api/ordens").header("Idempotency-Key", "curta"), json.readTree(corpo), admin, 400);
        assertThat(ordensRepository.count()).isZero();
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
        executar(get("/api/auth/me").cookie(segundo.cookie)).andExpect(status().isUnauthorized());
        Acesso outro = login("outro");
        enviar(patch("/api/usuarios/" + usuario(3).getId() + "/status"), Map.of("ativo", false), login("admin"), 200);
        executar(get("/api/auth/me").cookie(outro.cookie)).andExpect(status().isUnauthorized());
    }

    @Test void reativacaoExigeNovoLoginEmTodosOsDispositivos() throws Exception {
        Acesso primeiro = login("tecnico");
        Acesso segundo = login("tecnico");
        Acesso outraConta = login("outro");
        Acesso admin = login("admin");
        String status = "/api/usuarios/" + usuario(2).getId() + "/status";
        enviar(patch(status), Map.of("ativo", false), admin, 200);
        enviar(get("/api/auth/me"), null, primeiro, 401);
        enviar(post("/api/auth/login"), Map.of("usuario", "tecnico", "senha", SENHA), null, 401);
        enviar(patch(status), Map.of("ativo", true), admin, 200);
        enviar(get("/api/auth/me"), null, primeiro, 401);
        // Este dispositivo não fez nenhuma requisição durante a desativação.
        enviar(get("/api/auth/me"), null, segundo, 401);
        enviar(post("/api/auth/logout"), null, segundo, 401);
        enviar(get("/api/auth/me"), null, outraConta, 200);
        enviar(get("/api/auth/me"), null, admin, 200);
        Acesso novo = login("tecnico");
        enviar(get("/api/auth/me"), null, novo, 200);
        enviar(post("/api/auth/logout"), null, novo, 200);
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
    private ResultActions executar(MockHttpServletRequestBuilder requisicao) throws Exception {
        // MockMvc não preenche servletPath como o Tomcat no mapeamento padrão "/".
        return mvc.perform(requisicao.with(req -> {
            req.setServletPath(req.getRequestURI().substring(req.getContextPath().length()));
            if (req.getMethod().equals("POST") && req.getServletPath().equals("/api/ordens")
                    && req.getHeader("Idempotency-Key") == null) req.addHeader("Idempotency-Key", chave());
            return req;
        }));
    }
    private Acesso login(String nome) throws Exception {
        MvcResult resultado = executar(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("usuario", nome, "senha", SENHA)))).andExpect(status().isOk()).andReturn();
        String cabecalho = resultado.getResponse().getHeader("Set-Cookie");
        assertThat(cabecalho).contains("HttpOnly", "SameSite=Strict");
        String valor = cabecalho.split(";", 2)[0].split("=", 2)[1];
        return new Acesso(new Cookie("fluxoos_sessao", valor), json.readTree(resultado.getResponse().getContentAsString()).get("csrf").asText());
    }
    private JsonNode enviar(MockHttpServletRequestBuilder requisicao, Object corpo, Acesso acesso, int statusEsperado) throws Exception {
        if (acesso != null) requisicao.cookie(acesso.cookie).header("X-CSRF-Token", acesso.csrf);
        if (corpo != null) requisicao.contentType("application/json").content(json.writeValueAsString(corpo));
        String resposta = executar(requisicao).andExpect(status().is(statusEsperado)).andReturn().getResponse().getContentAsString();
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
