package br.com.fluxoos;

import br.com.fluxoos.dto.AtualizarUsuarioDTO;
import br.com.fluxoos.entity.*;
import br.com.fluxoos.enums.*;
import br.com.fluxoos.repository.*;
import br.com.fluxoos.seguranca.SenhaPbkdf2;
import br.com.fluxoos.service.*;
import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties = {
    "spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:rbac-tenant;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${TEST_DATABASE_USER:sa}", "spring.datasource.password=${TEST_DATABASE_PASSWORD:}",
    "fluxoos.bootstrap-password=Senha-multitenant-testes-2026!", "fluxoos.demo=false", "fluxoos.seguranca.max-sessoes=200"
})
@AutoConfigureMockMvc
class MultiTenantRbacTest {
    static final String SENHA="Senha-multitenant-testes-2026!";
    static final String HASH=new SenhaPbkdf2().gerarHash(SENHA.toCharArray());
    @Autowired MockMvc mvc; @Autowired ObjectMapper json;
    @Autowired EmpresaRepository empresas; @Autowired UsuarioRepository usuarios;
    @Autowired OrdemServicoRepository ordens; @Autowired NotificacaoRepository notificacoes;
    @Autowired OrdemServicoService servico; @Autowired UsuarioService usuarioService;
    @Autowired EstoqueService estoque; @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transacoes;
    Empresa a,b; Usuario admin,adminB,misto,externo,estoquista,supervisor,master;
    @BeforeEach void preparar() {
        a=empresas.save(new Empresa("Empresa A")); b=empresas.save(new Empresa("Empresa B"));
        admin=conta(a,Role.ROLE_ADMIN_EMPRESA); adminB=conta(b,Role.ROLE_ADMIN_EMPRESA);
        misto=conta(a,Role.ROLE_SUPORTE_TECNICO,Role.ROLE_ESTOQUISTA);
        externo=conta(a,Role.ROLE_SUPORTE_EXTERNO); estoquista=conta(a,Role.ROLE_ESTOQUISTA);
        supervisor=conta(a,Role.ROLE_SUPERVISOR); master=conta(null,Role.ROLE_ADMIN_MASTER);
    }
    Usuario conta(Empresa empresa,Role... roles) {
        return usuarios.save(new Usuario("u-"+UUID.randomUUID(),"Pessoa",empresa==null?null:empresa.getId(),Set.of(roles),HASH));
    }
    Map<String,Object> novoUsuario(Object roles) {
        return Map.of("login","novo-"+UUID.randomUUID(),"nome","Nova pessoa","senha",SENHA,"roles",roles);
    }
    Map<String,Object> novaOs() { return Map.of("solicitante","Ana","local","Bancada","problema","Diagnóstico","prioridade","MEDIA"); }
    @Test void adminCriaMultiplasRolesNoProprioTenantESemVazarHash() throws Exception {
        var acesso=login(admin);
        var u=enviar(post("/api/usuarios"),novoUsuario(List.of("ROLE_ESTOQUISTA","ROLE_SUPORTE_EXTERNO")),acesso,201);
        assertThat(u.get("empresaId").asLong()).isEqualTo(a.getId());
        assertThat(u.get("roles")).hasSize(2);
        assertThat(u.toString()).doesNotContain("hash", "password", "senha", "authorities");
        var corpo=new HashMap<>(novoUsuario(List.of("ROLE_ESTOQUISTA"))); corpo.put("empresaId",b.getId());
        enviar(post("/api/usuarios"),corpo,acesso,403);
        for (String role:List.of("ROLE_ADMIN_MASTER","ROLE_ADMIN_EMPRESA")) enviar(post("/api/usuarios"),novoUsuario(List.of(role)),acesso,403);
        enviar(post("/api/usuarios"),novoUsuario(List.of()),acesso,400);
        enviar(post("/api/usuarios"),novoUsuario(List.of("ROLE_INVENTADA")),acesso,400);
    }
    @Test void edicaoNaoPermiteTrocaDeTenantNemEscaladaERevogaSessao() throws Exception {
        var ac=login(admin); var antiga=login(misto);
        enviar(put("/api/usuarios/"+misto.getId()),Map.of("nome","Atualizada","roles",List.of("ROLE_SUPORTE_EXTERNO","ROLE_ESTOQUISTA"),"empresaId",b.getId()),ac,403);
        enviar(put("/api/usuarios/"+misto.getId()),Map.of("nome","Atualizada","roles",List.of("ROLE_ADMIN_MASTER")),ac,403);
        enviar(put("/api/usuarios/"+adminB.getId()),Map.of("nome","Invasão","roles",List.of("ROLE_ESTOQUISTA")),ac,404);
        enviar(patch("/api/usuarios/"+adminB.getId()+"/status"),Map.of("ativo",false),ac,404);
        enviar(put("/api/usuarios/"+misto.getId()),Map.of("nome","Atualizada","roles",List.of("ROLE_SUPORTE_EXTERNO","ROLE_ESTOQUISTA")),ac,200);
        enviar(get("/api/auth/me"),null,antiga,401);
        assertThat(usuarios.findById(misto.getId()).orElseThrow().getEmpresaId()).isEqualTo(a.getId());
    }
    @Test void listarUsuariosEOrdensSempreRestringeEmpresa() throws Exception {
        long oa=servico.abrir(admin,"A","A","A",Prioridade.MEDIA).getId();
        long ob=servico.abrir(adminB,"B","B","B",Prioridade.MEDIA).getId();
        var ac=login(admin);
        var lista=enviar(get("/api/usuarios").header("X-Empresa-Id",b.getId()),null,ac,200);
        for(var u:lista) assertThat(u.get("empresaId").asLong()).isEqualTo(a.getId());
        var os=enviar(get("/api/ordens"),null,ac,200);
        assertThat(os).hasSize(1); assertThat(os.get(0).get("id").asLong()).isEqualTo(oa);
        for(String rota:List.of("","/historico","/assinatura")) enviar(get("/api/ordens/"+ob+rota),null,ac,404);
        enviar(post("/api/ordens/"+ob+"/inicio"),null,ac,404);
        assertThat(servico.listar(master)).extracting(br.com.fluxoos.dto.Contratos.OrdemView::id).contains(oa,ob);
    }
    @Test void rolesAcumuladasCriamOsENotificamSomenteAdminsDaMesmaEmpresaUmaVez() throws Exception {
        var segundo=conta(a,Role.ROLE_ADMIN_EMPRESA,Role.ROLE_SUPERVISOR);
        var ac=login(misto); String chave=UUID.randomUUID().toString();
        var primeira=enviar(post("/api/ordens").header("Idempotency-Key",chave),novaOs(),ac,201);
        enviar(post("/api/ordens").header("Idempotency-Key",chave),novaOs(),ac,201);
        var na=notificacoes.findByEmpresaIdAndDestinatarioIdOrderByIdDesc(a.getId(),admin.getId());
        assertThat(na).hasSize(1); assertThat(na.getFirst().getOrdemId()).isEqualTo(primeira.get("id").asLong());
        assertThat(notificacoes.findByEmpresaIdAndDestinatarioIdOrderByIdDesc(a.getId(),segundo.getId())).hasSize(1);
        assertThat(notificacoes.findByEmpresaIdAndDestinatarioIdOrderByIdDesc(b.getId(),adminB.getId())).isEmpty();
        enviar(post("/api/ordens"),novaOs(),login(supervisor),201);
        enviar(post("/api/ordens"),novaOs(),login(estoquista),403);
        enviar(post("/api/ordens"),novaOs(),login(externo),403);
        enviar(post("/api/ordens"),novaOs(),login(admin),201);
        assertThat(notificacoes.findByEmpresaIdAndDestinatarioIdOrderByIdDesc(a.getId(),admin.getId())).hasSize(2);
    }
    @Test void roleSuporteTecnicoComOutrasFuncoesPodeSerAtribuidaMasOutroTenantNao() throws Exception {
        long os=servico.abrir(admin,"A","A","A",Prioridade.MEDIA).getId(); var ac=login(admin);
        enviar(post("/api/ordens/"+os+"/atribuicao"),Map.of("tecnicoId",adminB.getId()),ac,404);
        enviar(post("/api/ordens/"+os+"/atribuicao"),Map.of("tecnicoId",estoquista.getId()),ac,400);
        enviar(post("/api/ordens/"+os+"/atribuicao"),Map.of("tecnicoId",misto.getId()),login(supervisor),403);
        enviar(post("/api/ordens/"+os+"/atribuicao"),Map.of("tecnicoId",misto.getId()),ac,200);
        enviar(post("/api/ordens/"+os+"/inicio"),null,login(misto),200);
        enviar(post("/api/ordens/"+os+"/inicio"),null,login(externo),403);
    }
    @Test void rollbackDaOsTambemReverteTodasNotificacoes() {
        long antes=ordens.count(); long avisos=notificacoes.count();
        new TransactionTemplate(transacoes).executeWithoutResult(status -> {
            servico.abrir(misto,"Rollback","A","A",Prioridade.MEDIA);
            assertThat(notificacoes.count()).isEqualTo(avisos+1);
            status.setRollbackOnly();
        });
        assertThat(ordens.count()).isEqualTo(antes); assertThat(notificacoes.count()).isEqualTo(avisos);
    }
    @Test void estoqueTambemNaoVazaEntreTenants() throws Exception {
        var material=estoque.cadastrar(adminB,"B-"+UUID.randomUUID(),"Material B","un",0);
        var ac=login(admin);
        assertThat(enviar(get("/api/materiais"),null,ac,200)).isEmpty();
        enviar(post("/api/materiais/"+material.getId()+"/movimentos"),Map.of("tipo","ENTRADA","variacao",1,"observacao","Invasão"),ac,404);
        var dono=estoque.cadastrarEstoque(adminB,"B-"+UUID.randomUUID(),"Proprietário B",TipoEstoque.EMPRESA);
        enviar(post("/api/materiais"),Map.of("codigo","A-"+UUID.randomUUID(),"nome","Material","unidade","un","minimo",0,"estoqueId",dono.getId()),ac,404);
    }
    @Test void notificacoesExigemDestinatarioETenant() throws Exception {
        servico.abrir(misto,"A","A","A",Prioridade.MEDIA);
        var ac=login(admin);var lista=enviar(get("/api/notificacoes"),null,ac,200);
        assertThat(lista).hasSize(1); long id=lista.get(0).get("id").asLong();
        enviar(patch("/api/notificacoes/"+id+"/leitura"),null,login(adminB),404);
        enviar(get("/api/notificacoes"),null,login(estoquista),403);
        enviar(patch("/api/notificacoes/"+id+"/leitura"),null,ac,200);
        assertThat(enviar(get("/api/notificacoes"),null,ac,200).get(0).get("lida").asBoolean()).isTrue();
    }
    @Test void masterProvisionaEmpresasEAdminsSemPermitirQueAdminEmpresaVireMaster() throws Exception {
        var ac=login(master);
        var e=enviar(post("/api/plataforma/empresas"),Map.of("nome","Nova empresa"),ac,201);
        var corpo=new HashMap<>(novoUsuario(List.of("ROLE_ADMIN_EMPRESA"))); corpo.put("empresaId",e.get("id").asLong());
        enviar(post("/api/usuarios"),corpo,ac,201);
        enviar(post("/api/plataforma/empresas"),Map.of("nome","Negada"),login(admin),403);
        assertThatThrownBy(()->usuarioService.editar(master,admin.getId(),new AtualizarUsuarioDTO("Pessoa",Set.of(Role.ROLE_ESTOQUISTA),null))).isInstanceOf(IllegalStateException.class);
    }
    @Test void bancoRecusaVinculosCruzadosMesmoPorSqlDireto() {
        long os=servico.abrir(admin,"A","A","A",Prioridade.MEDIA).getId();
        assertThatThrownBy(()->jdbc.update("UPDATE ordens SET equipe_id=? WHERE id=?",adminB.getId(),os)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("INSERT INTO usuario_roles(usuario_id,role) VALUES(?,'ROLE_INVENTADA')",admin.getId())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("INSERT INTO notificacoes(id,empresa_id,ordem_id,destinatario_id,mensagem,criado_em,lida) VALUES(nextval('sequencia_ids'),?,?,?,'Cruzada',CURRENT_TIMESTAMP,false)",a.getId(),os,adminB.getId())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    record Acesso(Cookie cookie,String csrf) {}
    Acesso login(Usuario u) throws Exception {
        var r=executar(post("/api/auth/login").contentType("application/json").content(json.writeValueAsBytes(Map.of("usuario",u.getNomeUsuario(),"senha",SENHA)))).andReturn().getResponse();
        assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(200);
        return new Acesso(r.getCookie("fluxoos_sessao"),json.readTree(r.getContentAsByteArray()).get("csrf").asText());
    }
    JsonNode enviar(MockHttpServletRequestBuilder req,Object corpo,Acesso ac,int esperado) throws Exception {
        req.cookie(ac.cookie()).header("X-CSRF-Token",ac.csrf());
        var rascunho=req.buildRequest(new org.springframework.mock.web.MockServletContext());
        if(!"GET".equals(rascunho.getMethod()) && rascunho.getHeader("Idempotency-Key")==null) req.header("Idempotency-Key",UUID.randomUUID().toString());
        if(corpo!=null)req.contentType("application/json").content(json.writeValueAsBytes(corpo));
        var resposta=executar(req).andReturn().getResponse();
        assertThat(resposta.getStatus()).as(resposta.getContentAsString()).isEqualTo(esperado);
        return resposta.getContentAsByteArray().length==0?json.nullNode():json.readTree(resposta.getContentAsByteArray());
    }
    ResultActions executar(MockHttpServletRequestBuilder req) throws Exception {
        // Simula o servletPath preenchido pelo Tomcat no mapeamento "/".
        return mvc.perform(req.with(r -> { r.setServletPath(r.getRequestURI()); return r; }));
    }
}
