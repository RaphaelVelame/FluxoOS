package br.com.fluxoos.dto;

import br.com.fluxoos.entity.Usuario;
import br.com.fluxoos.enums.Role;
import java.util.Set;

public record UsuarioResponseDTO(long id, String login, String nome, Long empresaId, Set<Role> roles, boolean ativo) {
    public static UsuarioResponseDTO de(Usuario u) {
        return new UsuarioResponseDTO(u.getId(), u.getNomeUsuario(), u.getNomeCompleto(), u.getEmpresaId(), u.getRoles(), u.isAtivo());
    }
}
