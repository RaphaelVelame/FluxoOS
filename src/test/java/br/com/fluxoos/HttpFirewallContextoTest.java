package br.com.fluxoos;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:fluxoos-firewall-contexto;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "fluxoos.bootstrap-password=Senha-firewall-testes-2026!", "fluxoos.demo=false",
        "server.address=127.0.0.1", "fluxoos.cookie-secure=false", "server.servlet.context-path=/fluxo"
})
class HttpFirewallContextoTest extends HttpFirewallTest {
    @Override protected String contexto() { return "/fluxo"; }
}
