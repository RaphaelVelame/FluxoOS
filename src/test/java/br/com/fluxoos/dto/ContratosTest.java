package br.com.fluxoos.dto;

import br.com.fluxoos.enums.Perfil;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class ContratosTest {
    @Test void representacoesDeLogNaoRevelamCredenciaisTokensOuAssinatura() {
        String segredo = "segredo-que-nao-deve-ser-registrado";
        assertThat(new Contratos.Login("admin", segredo).toString()).doesNotContain(segredo);
        assertThat(new Contratos.NovaSenha(segredo, segredo).toString()).doesNotContain(segredo);
        assertThat(new Contratos.NovoUsuario("login", "Nome", Perfil.CHEFE, segredo).toString()).doesNotContain(segredo);
        assertThat(new CriarUsuarioDTO("login", "Nome", java.util.Set.of(br.com.fluxoos.enums.Role.ROLE_ESTOQUISTA), segredo, null).toString()).doesNotContain(segredo);
        assertThat(new Contratos.Conclusao("Nome", segredo).toString()).doesNotContain(segredo);
        assertThat(new Contratos.AcessoView(null, segredo, Instant.now()).toString()).doesNotContain(segredo);
    }
}
