package br.com.fluxoos.application;

import br.com.fluxoos.application.porta.*;
import br.com.fluxoos.application.seguranca.Identificadores;
import br.com.fluxoos.domain.modelo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UsuarioService {
    private final AdministracaoUsuarios cadastro;
    private final RepositorioOperacional dados;
    private final RepositorioUsuarios usuarios;
    private final HasherSenha hasher;

    public UsuarioService(AdministracaoUsuarios cadastro, RepositorioOperacional dados, RepositorioUsuarios usuarios, HasherSenha hasher) {
        this.cadastro = cadastro;
        this.dados = dados;
        this.usuarios = usuarios;
        this.hasher = hasher;
    }

    public Usuario cadastrar(Usuario ator, String login, String nome, Perfil perfil, char[] senha) {
        exigirChefia(ator);
        String normalizado = Identificadores.normalizarUsuario(login);
        if (!normalizado.matches("[a-z0-9._-]{3,80}")) throw new IllegalArgumentException("Use de 3 a 80 letras, números, pontos ou traços no login.");
        Usuario usuario = new Usuario(dados.proximoId(), normalizado, nome, perfil, true);
        cadastro.inserir(usuario, hasher.gerarHash(senha));
        dados.auditar(null, ator.getId(), "USUARIO_CRIADO", "Conta criada: " + normalizado);
        return usuario;
    }

    public void alterarStatus(Usuario ator, long id, boolean ativo) {
        exigirChefia(ator);
        if (id == ator.getId()) throw new IllegalArgumentException("Não é permitido alterar o status da própria conta.");
        usuarios.buscarPorId(id).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));
        cadastro.alterarStatus(id, ativo);
        dados.auditar(null, ator.getId(), "STATUS_USUARIO", "Conta " + id + (ativo ? " ativada." : " desativada."));
    }

    public static void exigirChefia(Usuario ator) {
        if (ator == null || !ator.isAtivo() || ator.getPerfil() != Perfil.CHEFE) throw new SecurityException("Acesso restrito à chefia.");
    }
}
