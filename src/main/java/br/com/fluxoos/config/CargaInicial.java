package br.com.fluxoos.config;

import br.com.fluxoos.repository.*;
import br.com.fluxoos.entity.*;
import br.com.fluxoos.enums.*;
import br.com.fluxoos.seguranca.SenhaPbkdf2;
import br.com.fluxoos.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.UUID;

@Component
public class CargaInicial implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final EstoqueService estoque;
    private final OrdemServicoService ordens;
    private final AuditoriaService auditoria;
    private final SenhaPbkdf2 hasher;
    private final String senha;
    private final boolean demo;

    public CargaInicial(UsuarioRepository usuarios, EstoqueService estoque, OrdemServicoService ordens,
                        AuditoriaService auditoria, SenhaPbkdf2 hasher,
                        @Value("${fluxoos.bootstrap-password}") String senha, @Value("${fluxoos.demo}") boolean demo) {
        this.usuarios = usuarios; this.estoque = estoque; this.ordens = ordens; this.auditoria = auditoria;
        this.hasher = hasher; this.senha = senha; this.demo = demo;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) return;
        if (senha.length() < 15) {
            throw new IllegalStateException("Primeira execução: defina BOOTSTRAP_PASSWORD com pelo menos 15 caracteres.");
        }
        char[] segredo = senha.toCharArray();
        String hash;
        try { hash = hasher.gerarHash(segredo); } finally { Arrays.fill(segredo, '\0'); }
        Usuario chefe = usuarios.save(new Usuario("admin", "Administrador", Perfil.CHEFE, hash));
        auditoria.registrar(null, chefe.getId(), "INICIALIZACAO", "Conta administrativa inicial criada.");
        if (!demo) return;
        Usuario tecnico = usuarios.save(new Usuario("tecnico", "Marcos Oliveira", Perfil.TECNICO, hash));
        Material cabo = new Material("MAT-001", "Cabo de rede CAT6", "m", 80, 20, true);
        Material conector = new Material("MAT-002", "Conector RJ45", "un", 8, 15, true);
        Material tomada = new Material("MAT-003", "Tomada de rede", "un", 24, 10, true);
        for (Material m : java.util.List.of(cabo, conector, tomada)) {
            // O saldo inicial também gera um registro de entrada rastreável.
            Material vazio = estoque.cadastrar(chefe, m.getCodigo(), m.getNome(), m.getUnidade(), m.getQuantidadeMinima());
            estoque.movimentar(chefe, vazio.getId(), TipoMovimento.ENTRADA, m.getQuantidade(),
                    "Carga de demonstração", UUID.randomUUID().toString());
        }
        String[][] exemplos = {{"Ana Santos", "Escola Municipal • Sala de informática", "Pontos de rede sem conexão", "ALTA"},
                {"Paulo Lima", "Unidade de Saúde • Recepção", "Instalação de estação de atendimento", "MEDIA"},
                {"Carla Souza", "Secretaria de Educação • Sala 04", "Organização do cabeamento", "BAIXA"}};
        for (String[] exemplo : exemplos) {
            OrdemServico ordem = ordens.abrir(chefe, exemplo[0], exemplo[1], exemplo[2], Prioridade.valueOf(exemplo[3]));
            ordens.atribuir(chefe, ordem.getId(), tecnico.getId());
        }
    }
}
