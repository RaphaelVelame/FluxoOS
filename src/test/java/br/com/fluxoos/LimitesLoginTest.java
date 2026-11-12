package br.com.fluxoos;

import br.com.fluxoos.entity.Usuario;
import br.com.fluxoos.enums.Perfil;
import br.com.fluxoos.repository.UsuarioRepository;
import br.com.fluxoos.seguranca.SenhaPbkdf2;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import br.com.fluxoos.repository.SessoesEmMemoria;
import br.com.fluxoos.seguranca.LimitadorTentativas;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:limites-login;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
        "spring.datasource.username=${TEST_DATABASE_USER:sa}", "spring.datasource.password=${TEST_DATABASE_PASSWORD:}",
        "fluxoos.bootstrap-password=Senha-limites-testes-2026!", "fluxoos.demo=false",
        "fluxoos.cookie-secure=false", "server.address=127.0.0.1",
        "fluxoos.seguranca.limpeza-intervalo-ms=50"
})
@Import(LimitesLoginTest.Config.class)
class LimitesLoginTest {
    private static final String SENHA = "Senha-limites-testes-2026!";
    private static final String HASH = new SenhaPbkdf2().gerarHash(SENHA.toCharArray());
    @LocalServerPort int porta;
    @Autowired UsuarioRepository usuarios;
    @Autowired RelogioTeste relogio;
    @Autowired SessoesEmMemoria sessoes;
    @Autowired LimitadorTentativas tentativas;
    @Autowired ObjectMapper json;
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private String login;

    @BeforeEach void preparar() {
        relogio.avancar(Duration.ofDays(1));
        login = "limite-" + UUID.randomUUID();
        usuarios.save(new Usuario(login, "Conta limites", Perfil.CHEFE, HASH));
    }

    @Test void aceitaVinteSessoesRecusaVigesimaPrimeiraELiberaVagaNoLogout() throws Exception {
        List<Acesso> acessos = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            if (i == 10) {
                login = "outra-" + UUID.randomUUID();
                usuarios.save(new Usuario(login, "Outra conta limites", Perfil.CHEFE, HASH));
            }
            acessos.add(entrar());
        }
        assertThat(autenticar(SENHA).statusCode()).isEqualTo(429);
        for (Acesso acesso : acessos) assertThat(enviar("/api/auth/me", "GET", acesso).statusCode()).isEqualTo(200);
        assertThat(autenticar("senha-incorreta").statusCode()).isEqualTo(401);
        assertThat(enviar("/api/auth/logout", "POST", acessos.get(0)).statusCode()).isEqualTo(200);
        assertThat(enviar("/api/auth/me", "GET", acessos.get(0)).statusCode()).isEqualTo(401);
        assertThat(enviar("/api/auth/me", "GET", entrar()).statusCode()).isEqualTo(200);
        assertThat(enviar("/api/auth/me", "GET", acessos.get(1)).statusCode()).isEqualTo(200);
    }

    @Test void cincoErrosBloqueiamPorCincoMinutosSemProrrogarComNovasTentativas() throws Exception {
        for (int i = 0; i < 5; i++) assertThat(autenticar("senha-incorreta").statusCode()).isEqualTo(401);
        assertThat(autenticar(SENHA).statusCode()).isEqualTo(401);
        relogio.avancar(Duration.ofMinutes(5).minusSeconds(1));
        assertThat(autenticar(SENHA).statusCode()).isEqualTo(401);
        relogio.avancar(Duration.ofSeconds(1));
        assertThat(autenticar(SENHA).statusCode()).isEqualTo(200);
        for (int i = 0; i < 4; i++) assertThat(autenticar("senha-incorreta").statusCode()).isEqualTo(401);
        assertThat(autenticar(SENHA).statusCode()).isEqualTo(200);
    }

    @Test void manutencaoDescartaSessoesETentativasSemNovasRequisicoes() throws Exception {
        entrar();
        assertThat(autenticar("senha-incorreta").statusCode()).isEqualTo(401);
        assertThat(tentativas.quantidadeNomes()).isPositive();
        relogio.avancar(Duration.ofMinutes(31));
        aguardar(() -> tentativas.quantidadeNomes() == 0 && tentativas.quantidadeIps() == 0);
        assertThat(tentativas.quantidadeNomes()).isZero();
        assertThat(tentativas.quantidadeIps()).isZero();
        assertThat(sessoes.quantidade()).isZero();
    }

    @Test void manutencaoRemoveContaDesativadaEReativacaoNaoRestauraSessao() throws Exception {
        Acesso antigo = entrar();
        Usuario conta = usuarios.findByNomeUsuario(login).orElseThrow();
        conta.desativar();
        usuarios.save(conta);
        aguardar(() -> sessoes.quantidade() == 0);
        assertThat(sessoes.quantidade()).isZero();
        conta = usuarios.findByNomeUsuario(login).orElseThrow();
        conta.ativar();
        usuarios.save(conta);
        assertThat(enviar("/api/auth/me", "GET", antigo).statusCode()).isEqualTo(401);
        assertThat(enviar("/api/auth/me", "GET", entrar()).statusCode()).isEqualTo(200);
    }

    @Test void limiteNaoMantemVagasDeSessoesExpiradas() throws Exception {
        Acesso antigo = null;
        for (int i = 0; i < 20; i++) antigo = entrar();
        assertThat(autenticar(SENHA).statusCode()).isEqualTo(429);
        relogio.avancar(Duration.ofMinutes(30));
        assertThat(autenticar(SENHA).statusCode()).isEqualTo(200);
        assertThat(sessoes.quantidade()).isEqualTo(1);
        assertThat(enviar("/api/auth/me", "GET", antigo).statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> autenticar(String senha) throws Exception {
        return cliente.send(HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("usuario", login, "senha", senha))))
                .build(), HttpResponse.BodyHandlers.ofString());
    }
    private Acesso entrar() throws Exception {
        var resposta = autenticar(SENHA);
        assertThat(resposta.statusCode()).isEqualTo(200);
        return new Acesso(resposta.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0],
                json.readTree(resposta.body()).get("csrf").asText());
    }
    private HttpResponse<String> enviar(String caminho, String metodo, Acesso acesso) throws Exception {
        return cliente.send(HttpRequest.newBuilder(uri(caminho)).header("Cookie", acesso.cookie())
                .header("X-CSRF-Token", acesso.csrf()).method(metodo, HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }
    private URI uri(String caminho) { return URI.create("http://127.0.0.1:" + porta + caminho); }
    private void aguardar(java.util.function.BooleanSupplier condicao) throws InterruptedException {
        long prazo = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (!condicao.getAsBoolean() && System.nanoTime() < prazo) Thread.sleep(10);
        assertThat(condicao.getAsBoolean()).as("Limpeza agendada sem nova requisição").isTrue();
    }
    private record Acesso(String cookie, String csrf) {}

    @TestConfiguration static class Config {
        @Bean @Primary RelogioTeste relogioTeste() { return new RelogioTeste(); }
    }
}
