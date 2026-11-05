package br.com.fluxoos;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.assertThat;

class ArquiteturaTest {
    @Test void controllersDelegamPersistenciaETransacoesAosServices() throws Exception {
        try (var arquivos = Files.walk(Path.of("src/main/java/br/com/fluxoos/controller"))) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                assertThat(Files.readString(arquivo)).as(arquivo.toString())
                        .doesNotContain("br.com.fluxoos.repository", "jakarta.persistence", "@Transactional");
            }
        }
    }
}
