package br.com.fluxoos.service;

import br.com.fluxoos.entity.*;
import br.com.fluxoos.enums.Role;
import br.com.fluxoos.repository.EmpresaRepository;
import br.com.fluxoos.seguranca.EscopoEmpresa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @Transactional
public class EmpresaService {
    private final EmpresaRepository empresas;
    public EmpresaService(EmpresaRepository empresas) { this.empresas = empresas; }
    public record Resposta(long id, String nome) { static Resposta de(Empresa e) { return new Resposta(e.getId(), e.getNome()); } }
    @Transactional(readOnly = true)
    public List<Resposta> listar(Usuario ator) {
        EscopoEmpresa.exigirRole(ator, Role.ROLE_ADMIN_MASTER);
        return empresas.findAll().stream().map(Resposta::de).toList();
    }
    public Resposta criar(Usuario ator, String nome) {
        EscopoEmpresa.exigirRole(ator, Role.ROLE_ADMIN_MASTER);
        return Resposta.de(empresas.save(new Empresa(nome)));
    }
}
