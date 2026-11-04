package br.com.fluxoos.infrastructure.memoria;

import br.com.fluxoos.application.seguranca.Identificadores;
import br.com.fluxoos.application.porta.RepositorioUsuarios;

import br.com.fluxoos.domain.modelo.Usuario;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Cadastro de exemplo; em produção, controle a mutação no repositório transacional. */
public final class UsuariosEmMemoria implements RepositorioUsuarios {
    private final Map<Long, Usuario> porId = new ConcurrentHashMap<>();
    private final Map<String, Usuario> porNome = new ConcurrentHashMap<>();

    public synchronized void adicionar(Usuario usuario) {
        String chave = Identificadores.normalizarUsuario(usuario.getNomeUsuario());
        if (porId.containsKey(usuario.getId()) || porNome.containsKey(chave)) {
            throw new IllegalArgumentException("Usuário duplicado");
        }
        porId.put(usuario.getId(), usuario);
        porNome.put(chave, usuario);
    }
    @Override public Optional<Usuario> buscarPorNome(String nome) {
        return Optional.ofNullable(porNome.get(Identificadores.normalizarUsuario(nome)));
    }
    @Override public Optional<Usuario> buscarPorId(long id) { return Optional.ofNullable(porId.get(id)); }
}
