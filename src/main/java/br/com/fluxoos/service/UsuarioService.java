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
import java.util.Set;
import java.util.Objects;
import br.com.fluxoos.dto.*;
import br.com.fluxoos.seguranca.EscopoEmpresa;

@Service
@Transactional
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;
    private final SenhaPbkdf2 hasher;
    private final EmpresaRepository empresas;
    private static final Set<Role> OPERACIONAIS = Set.of(Role.ROLE_SUPERVISOR, Role.ROLE_ESTOQUISTA,
            Role.ROLE_SUPORTE_TECNICO, Role.ROLE_SUPORTE_EXTERNO);

    public UsuarioService(UsuarioRepository usuarios, AuditoriaService auditoria, SenhaPbkdf2 hasher, EmpresaRepository empresas) {
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.hasher = hasher;
        this.empresas = empresas;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listar(Usuario ator) {
        if (ator != null && ator.temRole(Role.ROLE_ADMIN_MASTER)) {
            return usuarios.findAllByOrderByNomeCompletoAsc().stream().map(UsuarioResponseDTO::de).toList();
        }
        return usuarios.findByEmpresaIdOrderByNomeCompletoAsc(EscopoEmpresa.exigir(ator)).stream().map(UsuarioResponseDTO::de).toList();
    }

    public Usuario cadastrar(Usuario ator, String login, String nome, Perfil perfil, char[] senha) {
        return criar(ator, login, nome, Usuario.rolesLegadas(perfil), senha, null);
    }

    public Usuario criar(Usuario ator, String login, String nome, Set<Role> roles, char[] senha, Long empresaInformada) {
        exigirChefia(ator);
        validarConcessao(ator, roles);
        boolean master = ator.temRole(Role.ROLE_ADMIN_MASTER);
        Long empresaId = master ? empresaInformada : EscopoEmpresa.exigir(ator);
        if (!master && empresaInformada != null && !Objects.equals(empresaId, empresaInformada)) throw new SecurityException("Empresa não pode ser alterada.");
        if (roles.contains(Role.ROLE_ADMIN_MASTER)) {
            if (empresaId != null) throw new IllegalArgumentException("Master não pertence a uma empresa.");
        } else {
            if (empresaId == null || !empresas.existsById(empresaId)) throw new NaoEncontradoException("Empresa não encontrada.");
        }
        String normalizado = Identificadores.normalizarUsuario(login);
        if (!normalizado.matches("[a-z0-9._-]{3,80}")) throw new IllegalArgumentException("Use de 3 a 80 letras, números, pontos ou traços no login.");
        Usuario usuario = usuarios.save(new Usuario(normalizado, nome, empresaId, roles, hasher.gerarHash(senha)));
        auditoria.registrar(null, ator.getId(), "USUARIO_CRIADO", "Conta criada: " + normalizado);
        return usuario;
    }

    public Usuario editar(Usuario ator, long id, AtualizarUsuarioDTO entrada) {
        exigirChefia(ator);
        validarConcessao(ator, entrada.roles());
        Usuario alvo = carregarGerenciavel(ator, id);
        if (entrada.empresaId() != null && !Objects.equals(alvo.getEmpresaId(), entrada.empresaId())) throw new SecurityException("Empresa não pode ser alterada.");
        protegerUltimoAdmin(alvo, alvo.isAtivo() && entrada.roles().contains(Role.ROLE_ADMIN_EMPRESA));
        alvo.editar(entrada.nome(), entrada.roles());
        auditoria.registrar(null, ator.getId(), "USUARIO_EDITADO", "Nome/funções alterados para conta " + id);
        return alvo;
    }

    private void validarConcessao(Usuario ator, Set<Role> roles) {
        if (roles == null || roles.isEmpty() || roles.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Informe ao menos uma função válida.");
        if (!ator.temRole(Role.ROLE_ADMIN_MASTER) && !OPERACIONAIS.containsAll(roles)) throw new SecurityException("Somente funções operacionais podem ser atribuídas.");
    }

    private Usuario carregarGerenciavel(Usuario ator, long id) {
        Usuario alvo = ator.temRole(Role.ROLE_ADMIN_MASTER)
                ? usuarios.buscarParaAtualizar(id).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."))
                : usuarios.buscarParaAtualizarNaEmpresa(id, EscopoEmpresa.exigir(ator)).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));
        if (!ator.temRole(Role.ROLE_ADMIN_MASTER) && (alvo.getRoles().contains(Role.ROLE_ADMIN_MASTER) || alvo.getRoles().contains(Role.ROLE_ADMIN_EMPRESA))) throw new SecurityException("Esta conta exige administração da plataforma.");
        return alvo;
    }

    private void protegerUltimoAdmin(Usuario alvo, boolean permaneceAdmin) {
        if (alvo.getEmpresaId() == null || !alvo.temRole(Role.ROLE_ADMIN_EMPRESA) || permaneceAdmin) return;
        empresas.bloquear(alvo.getEmpresaId()).orElseThrow(() -> new NaoEncontradoException("Empresa não encontrada."));
        if (usuarios.buscarAtivosPorRole(alvo.getEmpresaId(), Role.ROLE_ADMIN_EMPRESA).size() <= 1) throw new IllegalStateException("Mantenha ao menos um administrador ativo na empresa.");
    }

    public void alterarStatus(Usuario ator, long id, boolean ativo) {
        exigirChefia(ator);
        if (id == ator.getId()) throw new IllegalArgumentException("Não é permitido alterar o status da própria conta.");
        Usuario usuario = carregarGerenciavel(ator, id);
        protegerUltimoAdmin(usuario, ativo);
        if (ativo) usuario.ativar(); else usuario.desativar();
        auditoria.registrar(null, ator.getId(), "STATUS_USUARIO", "Conta " + id + (ativo ? " ativada." : " desativada."));
    }

    public static void exigirChefia(Usuario ator) {
        EscopoEmpresa.exigirRole(ator, Role.ROLE_ADMIN_MASTER, Role.ROLE_ADMIN_EMPRESA);
    }
}
