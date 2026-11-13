package br.com.fluxoos.dto;

import br.com.fluxoos.enums.Role;
import jakarta.validation.constraints.*;
import java.util.Set;

public record AtualizarUsuarioDTO(@NotBlank @Size(max=160) String nome,
        @NotEmpty Set<@NotNull Role> roles, @Positive Long empresaId) {}
