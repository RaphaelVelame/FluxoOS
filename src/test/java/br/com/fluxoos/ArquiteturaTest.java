package br.com.fluxoos;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.assertThat;

class ArquiteturaTest {
    @Test void dominioPermaneceIndependenteDeFrameworksEAdaptadores() throws Exception {
        try (var arquivos = Files.walk(Path.of("src/main/java/br/com/fluxoos/domain"))) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                assertThat(Files.readString(arquivo)).as(arquivo.toString())
                        .doesNotContain("import org.springframework", "import jakarta.", "import br.com.fluxoos.infrastructure", "import br.com.fluxoos.application");
            }
        }
    }
}
