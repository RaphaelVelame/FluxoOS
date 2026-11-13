package br.com.fluxoos.dto;

import br.com.fluxoos.enums.Role;
import jakarta.validation.constraints.*;
import java.util.Set;

public record CriarUsuarioDTO(@NotBlank @Size(min=3,max=80) String login,
        @NotBlank @Size(max=160) String nome, @NotEmpty Set<@NotNull Role> roles,
        @NotBlank @Size(min=15,max=1024) String senha, @Positive Long empresaId) {
    @Override public String toString() { return "CriarUsuarioDTO[credenciais=PROTEGIDAS]"; }
}
