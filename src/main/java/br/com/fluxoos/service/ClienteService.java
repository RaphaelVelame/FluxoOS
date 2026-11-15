package br.com.fluxoos.service;

import br.com.fluxoos.dto.Contratos.ClienteView;
import br.com.fluxoos.entity.*;
import br.com.fluxoos.enums.Role;
import br.com.fluxoos.repository.ClienteRepository;
import br.com.fluxoos.seguranca.EscopoEmpresa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class ClienteService {
    private final ClienteRepository clientes;
    private final AuditoriaService auditoria;

    public ClienteService(ClienteRepository clientes, AuditoriaService auditoria) {
        this.clientes = clientes;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<ClienteView> listar(Usuario ator) {
        return clientes.findByEmpresaIdOrderByNomeAsc(EscopoEmpresa.exigir(ator)).stream().map(ClienteView::de).toList();
    }

    public Cliente cadastrar(Usuario ator, String codigo, String nome) {
        EscopoEmpresa.exigirRole(ator, Role.ROLE_ADMIN_EMPRESA, Role.ROLE_SUPERVISOR,
                Role.ROLE_SUPORTE_TECNICO, Role.ROLE_ESTOQUISTA);
        Cliente cliente = clientes.save(new Cliente(EscopoEmpresa.exigir(ator), codigo, nome));
        auditoria.registrar(null, ator.getId(), "CLIENTE_CRIADO", "Cliente cadastrado: " + cliente.getCodigo());
        return cliente;
    }

    @Transactional(readOnly = true)
    public Cliente carregar(Usuario ator, long id) {
        return clientes.findByIdAndEmpresaId(id, EscopoEmpresa.exigir(ator))
                .orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado."));
    }
}
