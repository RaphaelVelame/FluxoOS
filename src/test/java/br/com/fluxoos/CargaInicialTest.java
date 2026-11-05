package br.com.fluxoos;

import br.com.fluxoos.config.CargaInicial;
import br.com.fluxoos.enums.TipoMovimento;
import br.com.fluxoos.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fluxoos-demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "fluxoos.bootstrap-password=Senha-inicial-demo-2026!", "fluxoos.demo=true"
})
class CargaInicialTest {
    @Autowired CargaInicial carga;
    @Autowired UsuarioRepository usuarios;
    @Autowired MaterialRepository materiais;
    @Autowired OrdemServicoRepository ordens;
    @Autowired MovimentoEstoqueRepository movimentos;

    @Test void criaDemonstracaoComSaldosEMovimentosPersistidosESemDuplicarCarga() {
        assertThat(usuarios.count()).isEqualTo(2);
        assertThat(ordens.count()).isEqualTo(3);
        assertThat(materiais.findAllByOrderByNomeAsc()).extracting(m -> m.getQuantidade()).containsExactly(80, 8, 24);
        assertThat(movimentos.findAll()).hasSize(3).allSatisfy(m -> {
            assertThat(m.getTipoMovimento()).isEqualTo(TipoMovimento.ENTRADA);
            assertThat(m.getSaldoApos()).isEqualTo(m.getVariacao());
        });
        carga.run(new DefaultApplicationArguments());
        assertThat(usuarios.count()).isEqualTo(2);
        assertThat(ordens.count()).isEqualTo(3);
        assertThat(materiais.count()).isEqualTo(3);
        assertThat(movimentos.count()).isEqualTo(3);
    }
}
