package br.com.fluxoos.event;

public record OrdemServicoCriadaEvent(long empresaId, long ordemId, long autorId) {}
