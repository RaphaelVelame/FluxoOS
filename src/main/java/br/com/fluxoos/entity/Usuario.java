package br.com.fluxoos.entity;

import br.com.fluxoos.enums.Perfil;
import br.com.fluxoos.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_ids")
    @SequenceGenerator(name = "usuario_ids", sequenceName = "sequencia_ids", allocationSize = 1)
    private Long id;
    @Column(name = "nome_usuario", nullable = false, unique = true, length = 80)
    private String nomeUsuario;
    @Column(name = "nome_completo", nullable = false, length = 160)
    private String nomeCompleto;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Perfil perfil;
    @Column(name = "empresa_id", updatable = false)
    private Long empresaId;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 40)
    private Set<Role> roles = new HashSet<>();
    @Column(nullable = false)
    private boolean ativo;
    @JsonIgnore
    @Column(name = "versao_sessao", nullable = false)
    private long versaoSessao;
    @JsonIgnore
    @Column(name = "hash_senha", nullable = false, length = 300)
    private String hashSenha;

    protected Usuario() {}

    public Usuario(String nomeUsuario, String nomeCompleto, Perfil perfil, String hashSenha) {
        this.nomeUsuario = Validacoes.textoObrigatorio(nomeUsuario, "Nome de usuário");
        this.nomeCompleto = Validacoes.textoObrigatorio(nomeCompleto, "Nome completo");
        this.perfil = Objects.requireNonNull(perfil, "Perfil é obrigatório.");
        this.hashSenha = Validacoes.textoObrigatorio(hashSenha, "Hash da senha");
        this.ativo = true;
        this.empresaId = 1L;
        this.roles.addAll(rolesLegadas(perfil));
    }

    public Usuario(String login, String nome, Long empresaId, Set<Role> roles, String hash) {
        this(login, nome, perfilCompativel(roles), hash);
        validarRoles(empresaId, roles);
        this.empresaId = empresaId;
        this.roles = new HashSet<>(roles);
    }

    private static void validarRoles(Long empresaId, Set<Role> roles) {
        if (roles == null || roles.isEmpty() || roles.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Informe ao menos uma função válida.");
        if (roles.contains(Role.ROLE_ADMIN_MASTER)) {
            if (empresaId != null || roles.size() != 1) throw new IllegalArgumentException("Admin master possui escopo global exclusivo.");
        } else if (empresaId == null || empresaId <= 0) throw new IllegalArgumentException("Empresa obrigatória.");
    }

    public void editar(String nome, Set<Role> novasRoles) {
        validarRoles(empresaId, novasRoles);
        nomeCompleto = Validacoes.textoObrigatorio(nome, "Nome completo");
        if (!roles.equals(novasRoles)) {
            roles.clear(); roles.addAll(novasRoles);
            perfil = perfilCompativel(novasRoles);
            versaoSessao = Math.incrementExact(versaoSessao);
        }
    }

    public static Set<Role> rolesLegadas(Perfil perfil) {
        return switch (perfil) {
            case CHEFE -> Set.of(Role.ROLE_ADMIN_EMPRESA);
            case SUPORTE_TI -> Set.of(Role.ROLE_SUPORTE_TECNICO, Role.ROLE_ESTOQUISTA);
            case TECNICO -> Set.of(Role.ROLE_SUPORTE_EXTERNO);
            case SOLICITANTE -> Set.of(); // Consulta legada, sem concessão automática de privilégios.
        };
    }
    private static Perfil perfilCompativel(Set<Role> roles) {
        if (roles == null) return Perfil.SOLICITANTE;
        if (roles.contains(Role.ROLE_ADMIN_MASTER) || roles.contains(Role.ROLE_ADMIN_EMPRESA)) return Perfil.CHEFE;
        if (roles.contains(Role.ROLE_ESTOQUISTA)) return Perfil.SUPORTE_TI;
        if (roles.contains(Role.ROLE_SUPORTE_TECNICO) || roles.contains(Role.ROLE_SUPORTE_EXTERNO)) return Perfil.TECNICO;
        return Perfil.SOLICITANTE;
    }

    public Long getEmpresaId() { return empresaId; }
    public Set<Role> getRoles() { return Set.copyOf(roles); }
    public boolean temRole(Role role) { return ativo && roles.contains(role); }
    public boolean temAlgumaRole(Role... candidatas) { return ativo && Arrays.stream(candidatas).anyMatch(roles::contains); }
    @Override @JsonIgnore public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(r -> new SimpleGrantedAuthority(r.name())).toList();
    }
    @Override @JsonIgnore public String getPassword() { return hashSenha; }
    @Override @JsonIgnore public String getUsername() { return nomeUsuario; }
    @Override @JsonIgnore public boolean isEnabled() { return ativo; }
    @Override @JsonIgnore public boolean isAccountNonExpired() { return true; }
    @Override @JsonIgnore public boolean isAccountNonLocked() { return ativo; }
    @Override @JsonIgnore public boolean isCredentialsNonExpired() { return true; }

    public long getId() { return id; }
    public String getNomeUsuario() { return nomeUsuario; }
    public String getNomeCompleto() { return nomeCompleto; }
    public Perfil getPerfil() { return perfil; }
    public boolean isAtivo() { return ativo; }
    public long getVersaoSessao() { return versaoSessao; }

    public void ativar() { ativo = true; }
    public void desativar() {
        if (ativo) versaoSessao = Math.incrementExact(versaoSessao);
        ativo = false;
    }

    public boolean podeMovimentarEstoque() {
        return temAlgumaRole(Role.ROLE_ADMIN_EMPRESA, Role.ROLE_ESTOQUISTA);
    }
}
