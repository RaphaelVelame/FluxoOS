package br.com.fluxoos.dto;

import br.com.fluxoos.enums.Prioridade;
import jakarta.validation.constraints.*;

public record CriarOsDTO(@NotBlank @Size(max=160) String solicitante,
        @NotBlank @Size(max=240) String local, @NotBlank @Size(max=4000) String problema,
        @NotNull Prioridade prioridade) {}
