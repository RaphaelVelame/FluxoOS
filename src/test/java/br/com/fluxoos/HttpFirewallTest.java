package br.com.fluxoos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:fluxoos-firewall;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "fluxoos.bootstrap-password=Senha-firewall-testes-2026!", "fluxoos.demo=false",
        "server.address=127.0.0.1", "fluxoos.cookie-secure=false"
})
class HttpFirewallTest {
    @LocalServerPort int porta;
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    protected String contexto() { return ""; }

    @Test void consultasEAssinaturasExigemSessao() throws Exception {
        for (String caminho : List.of("/api", "/api/usuarios", "/api/ordens", "/api/materiais",
                "/api/movimentos", "/api/auth/me", "/api/ordens/100/assinatura")) {
            assertThat(enviar(HttpRequest.newBuilder(uri(caminho)).GET().build()).statusCode())
                    .as(caminho).isEqualTo(401);
        }
        for (String caminho : List.of("/api/ordens", "/api/auth/logout", "/api/auth/senha")) {
            assertThat(enviar(HttpRequest.newBuilder(uri(caminho))
                    .POST(HttpRequest.BodyPublishers.noBody()).build()).statusCode())
                    .as(caminho).isEqualTo(401);
        }
    }

    @Test void caminhosCodificadosNaoIgnoramAutenticacao() throws Exception {
        for (String caminho : List.of("/%61pi/usuarios", "/api/%75suarios", "/%61pi/auth/me")) {
            assertThat(enviar(HttpRequest.newBuilder(uri(caminho)).GET().build()).statusCode())
                    .as(caminho).isEqualTo(401);
        }
    }

    @Test void escritaExigeCsrfMesmoComSessaoValida() throws Exception {
        HttpResponse<String> login = enviar(HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"usuario\":\"admin\",\"senha\":\"Senha-firewall-testes-2026!\"}"))
                .build());
        assertThat(login.statusCode()).isEqualTo(200);
        String cookie = login.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
        String csrf = new ObjectMapper().readTree(login.body()).get("csrf").asText();
        String corpo = "{\"solicitante\":\"Teste\",\"local\":\"Escola\",\"problema\":\"Rede\",\"prioridade\":\"MEDIA\"}";
        assertThat(enviar(HttpRequest.newBuilder(uri("/api/ordens"))
                .header("Cookie", cookie).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo)).build()).statusCode()).isEqualTo(403);
        assertThat(enviar(HttpRequest.newBuilder(uri("/api/ordens"))
                .header("Cookie", cookie).header("Content-Type", "application/json")
                .header("X-CSRF-Token", csrf)
                .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(corpo)).build()).statusCode()).isEqualTo(201);
    }

    @Test void rejeitaMatrixParametersLiteraisECodificadosNoServidorReal() throws Exception {
        for (String caminho : List.of("/api;revisao/usuarios", "/api%3Brevisao/usuarios",
                "/api%3brevisao/usuarios", "/api/usuarios;revisao", "/api/usuarios%3Brevisao",
                "/api;revisao/ordens", "/api;revisao/materiais", "/api;revisao/movimentos")) {
            HttpResponse<String> resposta = enviar(HttpRequest.newBuilder(uri(caminho)).GET().build());
            assertThat(resposta.statusCode()).as(caminho).isEqualTo(400);
            assertThat(resposta.body()).doesNotContain("Administrador", "nomeCompleto", "hash_senha");
        }
    }

    @Test void preservaPaginaPublicaLoginESessaoNasRotasNormais() throws Exception {
        assertThat(enviar(HttpRequest.newBuilder(uri("/")).GET().build()).statusCode()).isEqualTo(200);
        assertThat(enviar(HttpRequest.newBuilder(uri("/api/usuarios")).GET().build()).statusCode()).isEqualTo(401);
        HttpResponse<String> login = enviar(HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"usuario\":\"admin\",\"senha\":\"Senha-firewall-testes-2026!\"}"))
                .build());
        assertThat(login.statusCode()).isEqualTo(200);
        String cookie = login.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
        assertThat(enviar(HttpRequest.newBuilder(uri("/api/usuarios")).header("Cookie", cookie).GET().build()).statusCode()).isEqualTo(200);
        assertThat(enviar(HttpRequest.newBuilder(uri("/api;revisao/usuarios")).header("Cookie", cookie).GET().build()).statusCode()).isEqualTo(400);
    }

    @Test void corpoEmBlocosTambemRespeitaLimiteDeBytes() throws Exception {
        // Publisher sem tamanho conhecido produz Transfer-Encoding: chunked em HTTP/1.1.
        String corpo = "{\"usuario\":\"admin\",\"senha\":\"" + "á".repeat(150_001) + "\"}";
        var publisher = HttpRequest.BodyPublishers.ofInputStream(() ->
                new ByteArrayInputStream(corpo.getBytes(StandardCharsets.UTF_8)));
        assertThat(publisher.contentLength()).isEqualTo(-1);
        var resposta = enviar(HttpRequest.newBuilder(uri("/api/auth/login"))
                .version(HttpClient.Version.HTTP_1_1).header("Content-Type","application/json")
                .POST(publisher).build());
        assertThat(resposta.statusCode()).isEqualTo(413);
    }

    @Test void corpoEmBlocosValidoContinuaDisponivelParaOController() throws Exception {
        String corpo="{\"usuario\":\"admin\",\"senha\":\"Senha-firewall-testes-2026!\"}";
        var login=enviar(HttpRequest.newBuilder(uri("/api/auth/login"))
                .version(HttpClient.Version.HTTP_1_1).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofInputStream(()->new ByteArrayInputStream(corpo.getBytes(StandardCharsets.UTF_8))))
                .build());
        assertThat(login.statusCode()).isEqualTo(200);
        String cookie=login.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];
        String csrf=new ObjectMapper().readTree(login.body()).get("csrf").asText();
        String estoque="{\"codigo\":\""+java.util.UUID.randomUUID()+"\",\"nome\":\"São Lucas\",\"tipo\":\"CLIENTE\"}";
        String estoqueNoLimite=" ".repeat(300_000-estoque.getBytes(StandardCharsets.UTF_8).length)+estoque;
        var criado=enviar(HttpRequest.newBuilder(uri("/api/estoques"))
                .version(HttpClient.Version.HTTP_1_1).header("Content-Type","application/json")
                .header("Cookie",cookie).header("X-CSRF-Token",csrf)
                .POST(HttpRequest.BodyPublishers.ofInputStream(()->new ByteArrayInputStream(estoqueNoLimite.getBytes(StandardCharsets.UTF_8))))
                .build());
        assertThat(criado.statusCode()).isEqualTo(201);
        assertThat(new ObjectMapper().readTree(criado.body()).get("nome").asText()).isEqualTo("São Lucas");
        assertThat(enviar(HttpRequest.newBuilder(uri("/api/auth/logout"))
                .header("Cookie",cookie).header("X-CSRF-Token",csrf)
                .POST(HttpRequest.BodyPublishers.noBody()).build()).statusCode()).isEqualTo(200);
    }

    @Test void errosDoProtocoloNaoViramFalhasInternas() throws Exception {
        var login=enviar(HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"usuario\":\"admin\",\"senha\":\"Senha-firewall-testes-2026!\"}"))
                .build());
        assertThat(login.statusCode()).isEqualTo(200);
        String cookie=login.headers().firstValue("Set-Cookie").orElseThrow().split(";",2)[0];
        String csrf=new ObjectMapper().readTree(login.body()).get("csrf").asText();
        try {
            var rota=enviar(HttpRequest.newBuilder(uri("/api/rota-inexistente")).header("Cookie",cookie).GET().build());
            assertThat(rota.statusCode()).isEqualTo(404);
            var metodo=enviar(HttpRequest.newBuilder(uri("/api/auth/login")).header("Cookie",cookie).GET().build());
            assertThat(metodo.statusCode()).isEqualTo(405);
            assertThat(metodo.headers().firstValue("Allow").orElseThrow()).contains("POST");
            var formato=enviar(HttpRequest.newBuilder(uri("/api/auth/login"))
                    .header("Content-Type","text/plain").POST(HttpRequest.BodyPublishers.ofString("{}" )).build());
            assertThat(formato.statusCode()).isEqualTo(415);
            var identificador=enviar(HttpRequest.newBuilder(uri("/api/ordens/abc")).header("Cookie",cookie).GET().build());
            assertThat(identificador.statusCode()).isEqualTo(400);
            assertThat(rota.body()+metodo.body()+formato.body()+identificador.body())
                    .doesNotContain("Exception", "stackTrace", "br.com.fluxoos");
        } finally {
            enviar(HttpRequest.newBuilder(uri("/api/auth/logout"))
                    .header("Cookie",cookie).header("X-CSRF-Token",csrf)
                    .POST(HttpRequest.BodyPublishers.noBody()).build());
        }
    }

    private URI uri(String caminho) { return URI.create("http://127.0.0.1:" + porta + contexto() + caminho); }
    private HttpResponse<String> enviar(HttpRequest requisicao) throws Exception {
        return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }
}
