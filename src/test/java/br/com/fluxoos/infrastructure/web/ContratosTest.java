package br.com.fluxoos.infrastructure.web;

import br.com.fluxoos.domain.modelo.Perfil;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class ContratosTest {
    @Test void representacoesDeLogNaoRevelamCredenciaisTokensOuAssinatura() {
        String segredo = "segredo-que-nao-deve-ser-registrado";
        assertThat(new Contratos.Login("admin", segredo).toString()).doesNotContain(segredo);
        assertThat(new Contratos.NovaSenha(segredo, segredo).toString()).doesNotContain(segredo);
        assertThat(new Contratos.NovoUsuario("login", "Nome", Perfil.CHEFE, segredo).toString()).doesNotContain(segredo);
        assertThat(new Contratos.Conclusao("Nome", segredo).toString()).doesNotContain(segredo);
        assertThat(new Contratos.AcessoView(null, segredo, Instant.now()).toString()).doesNotContain(segredo);
    }
}
