package br.com.fluxoos.service;

import br.com.fluxoos.repository.*;
import br.com.fluxoos.seguranca.Identificadores;
import br.com.fluxoos.seguranca.SenhaPbkdf2;
import br.com.fluxoos.dto.Contratos.UsuarioView;
import br.com.fluxoos.entity.*;
import br.com.fluxoos.enums.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;
    private final SenhaPbkdf2 hasher;

    public UsuarioService(UsuarioRepository usuarios, AuditoriaService auditoria, SenhaPbkdf2 hasher) {
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.hasher = hasher;
    }

    @Transactional(readOnly = true)
    public List<UsuarioView> listar() {
        return usuarios.findAllByOrderByNomeCompletoAsc().stream().map(UsuarioView::de).toList();
    }

    public Usuario cadastrar(Usuario ator, String login, String nome, Perfil perfil, char[] senha) {
        exigirChefia(ator);
        String normalizado = Identificadores.normalizarUsuario(login);
        if (!normalizado.matches("[a-z0-9._-]{3,80}")) throw new IllegalArgumentException("Use de 3 a 80 letras, números, pontos ou traços no login.");
        Usuario usuario = usuarios.save(new Usuario(normalizado, nome, perfil, hasher.gerarHash(senha)));
        auditoria.registrar(null, ator.getId(), "USUARIO_CRIADO", "Conta criada: " + normalizado);
        return usuario;
    }

    public void alterarStatus(Usuario ator, long id, boolean ativo) {
        exigirChefia(ator);
        if (id == ator.getId()) throw new IllegalArgumentException("Não é permitido alterar o status da própria conta.");
        Usuario usuario = usuarios.buscarParaAtualizar(id).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));
        if (ativo) usuario.ativar(); else usuario.desativar();
        auditoria.registrar(null, ator.getId(), "STATUS_USUARIO", "Conta " + id + (ativo ? " ativada." : " desativada."));
    }

    public static void exigirChefia(Usuario ator) {
        if (ator == null || !ator.isAtivo() || ator.getPerfil() != Perfil.CHEFE) throw new SecurityException("Acesso restrito à chefia.");
    }
}
