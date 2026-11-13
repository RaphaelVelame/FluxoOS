package br.com.fluxoos.dto;

import jakarta.validation.constraints.Positive;

public record AtribuirOsDTO(@Positive long tecnicoId) {}
