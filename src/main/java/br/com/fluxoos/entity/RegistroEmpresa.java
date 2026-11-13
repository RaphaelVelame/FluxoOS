package br.com.fluxoos.entity;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class RegistroEmpresa {
    // Construtores legados continuam exclusivos da empresa legada, nunca de todas as empresas.
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId = 1L;
    public Long getEmpresaId() { return empresaId; }
    protected final void vincularEmpresa(long empresaId) {
        if (empresaId <= 0) throw new IllegalArgumentException("Empresa inválida.");
        this.empresaId = empresaId;
    }
}
