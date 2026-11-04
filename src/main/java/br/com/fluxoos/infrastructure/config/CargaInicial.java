package br.com.fluxoos.infrastructure.config;

import br.com.fluxoos.application.porta.*;
import br.com.fluxoos.domain.modelo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;

@Component
public class CargaInicial implements ApplicationRunner {
    private final RepositorioOperacional dados;
    private final AdministracaoUsuarios usuarios;
    private final HasherSenha hasher;
    private final String senha;
    private final boolean demo;

    public CargaInicial(RepositorioOperacional dados, AdministracaoUsuarios usuarios, HasherSenha hasher,
                        @Value("${fluxoos.bootstrap-password}") String senha, @Value("${fluxoos.demo}") boolean demo) {
        this.dados = dados; this.usuarios = usuarios; this.hasher = hasher; this.senha = senha; this.demo = demo;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (!dados.listarUsuarios().isEmpty()) return;
        if (senha.length() < 15) {
            throw new IllegalStateException("Primeira execução: defina BOOTSTRAP_PASSWORD com pelo menos 15 caracteres.");
        }
        char[] segredo = senha.toCharArray();
        String hash;
        try { hash = hasher.gerarHash(segredo); } finally { Arrays.fill(segredo, '\0'); }
        Usuario chefe = new Usuario(dados.proximoId(), "admin", "Administrador", Perfil.CHEFE, true);
        usuarios.inserir(chefe, hash);
        dados.auditar(null, chefe.getId(), "INICIALIZACAO", "Conta administrativa inicial criada.");
        if (!demo) return;
        Usuario tecnico = new Usuario(dados.proximoId(), "tecnico", "Marcos Oliveira", Perfil.TECNICO, true);
        usuarios.inserir(tecnico, hash);
        Material cabo = new Material(dados.proximoId(), "MAT-001", "Cabo de rede CAT6", "m", 80, 20, true);
        Material conector = new Material(dados.proximoId(), "MAT-002", "Conector RJ45", "un", 8, 15, true);
        Material tomada = new Material(dados.proximoId(), "MAT-003", "Tomada de rede", "un", 24, 10, true);
        for (Material m : java.util.List.of(cabo, conector, tomada)) {
            // O saldo inicial também gera um registro de entrada rastreável.
            Material vazio = new Material(m.getId(), m.getCodigo(), m.getNome(), m.getUnidade(), 0, m.getQuantidadeMinima(), true);
            dados.inserirMaterial(vazio);
            MovimentoEstoque movimento = new EstoqueService().registrarMovimento(chefe, vazio, TipoMovimento.ENTRADA, m.getQuantidade(), "Carga de demonstração");
            dados.salvarMaterial(vazio);
            dados.inserirMovimento(movimento);
        }
        String[][] exemplos = {{"Ana Santos", "Escola Municipal • Sala de informática", "Pontos de rede sem conexão", "ALTA"},
                {"Paulo Lima", "Unidade de Saúde • Recepção", "Instalação de estação de atendimento", "MEDIA"},
                {"Carla Souza", "Secretaria de Educação • Sala 04", "Organização do cabeamento", "BAIXA"}};
        for (String[] exemplo : exemplos) {
            OrdemServico ordem = new OrdemServico(dados.proximoId(), exemplo[0], exemplo[1], exemplo[2], Prioridade.valueOf(exemplo[3]));
            dados.inserirOrdem(ordem);
            ordem.atribuirEquipe(chefe, tecnico);
            dados.salvarOrdem(ordem);
            dados.auditar(ordem.getId(), chefe.getId(), "ABERTURA", "Ordem demonstrativa aberta e atribuída.");
        }
    }
}
