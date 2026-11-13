package br.com.fluxoos.seguranca;

import br.com.fluxoos.entity.Usuario;
import br.com.fluxoos.enums.Role;
import br.com.fluxoos.service.NaoEncontradoException;
import java.util.Objects;

/** O tenant vem do principal autenticado, nunca de headers ou campos livres do cliente. */
public final class EscopoEmpresa {
    private EscopoEmpresa() {}
    public static long exigir(Usuario ator) {
        if (ator == null || !ator.isAtivo() || ator.getEmpresaId() == null) throw new SecurityException("Selecione uma conta operacional de empresa.");
        return ator.getEmpresaId();
    }
    public static void mesma(Usuario ator, Long empresaId) {
        if (!Objects.equals(exigir(ator), empresaId)) throw new NaoEncontradoException("Registro não encontrado.");
    }
    public static void exigirRole(Usuario ator, Role... roles) {
        if (ator == null || !ator.temAlgumaRole(roles)) throw new SecurityException("Sem permissão para esta operação.");
    }
}
