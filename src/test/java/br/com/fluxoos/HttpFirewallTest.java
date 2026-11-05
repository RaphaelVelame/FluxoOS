package br.com.fluxoos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:fluxoos-firewall;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "fluxoos.bootstrap-password=Senha-firewall-testes-2026!", "fluxoos.demo=false",
        "server.address=127.0.0.1", "fluxoos.cookie-secure=false"
})
class HttpFirewallTest {
    @LocalServerPort int porta;
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

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

    private URI uri(String caminho) { return URI.create("http://127.0.0.1:" + porta + caminho); }
    private HttpResponse<String> enviar(HttpRequest requisicao) throws Exception {
        return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }
}
