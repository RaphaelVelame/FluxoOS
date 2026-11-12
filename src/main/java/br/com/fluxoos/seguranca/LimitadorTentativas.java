package br.com.fluxoos.seguranca;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/** Em memória, por JVM. Em produção, transferir para armazenamento distribuído atômico. */
public final class LimitadorTentativas {
    private final Map<String, TentativaLogin> nomes = new HashMap<>();
    private final Map<String, TentativaLoginIP> ips = new HashMap<>();
    private final PoliticaBloqueio porUsuario;
    private final PoliticaBloqueio porIp;
    private final int capacidade;

    public LimitadorTentativas(PoliticaBloqueio porUsuario, PoliticaBloqueio porIp) {
        this(porUsuario, porIp, 10_000);
    }

    public LimitadorTentativas(PoliticaBloqueio porUsuario, PoliticaBloqueio porIp, int capacidade) {
        this.porUsuario = java.util.Objects.requireNonNull(porUsuario);
        this.porIp = java.util.Objects.requireNonNull(porIp);
        if (capacidade < 1) throw new IllegalArgumentException("Capacidade deve ser positiva.");
        this.capacidade = capacidade;
    }

    public synchronized boolean bloqueado(String nome, String ip, Instant agora) {
        limpar(agora);
        String usuario = Identificadores.normalizarUsuario(nome);
        String endereco = Identificadores.validarIp(ip);
        // Não expulsa bloqueios válidos para admitir identidades novas.
        if ((!nomes.containsKey(usuario) && nomes.size() >= capacidade)
                || (!ips.containsKey(endereco) && ips.size() >= capacidade)) return true;
        return (nomes.containsKey(usuario) && nomes.get(usuario).estaBloqueado(agora, porUsuario))
                || (ips.containsKey(endereco) && ips.get(endereco).estaBloqueado(agora, porIp));
    }
    public synchronized void falha(String nome, String ip, Instant agora) {
        limpar(agora);
        String usuario = Identificadores.normalizarUsuario(nome);
        String endereco = Identificadores.validarIp(ip);
        if (nomes.containsKey(usuario) || nomes.size() < capacidade) {
            nomes.computeIfAbsent(usuario, TentativaLogin::new).registrarFalha(agora, porUsuario);
        }
        if (ips.containsKey(endereco) || ips.size() < capacidade) {
            ips.computeIfAbsent(endereco, TentativaLoginIP::new).registrarFalha(agora, porIp);
        }
    }
    public synchronized void sucesso(String nome) { nomes.remove(Identificadores.normalizarUsuario(nome)); }
    public synchronized TentativaLogin consultarNome(String nome) {
        String chave = Identificadores.normalizarUsuario(nome);
        return nomes.getOrDefault(chave, new TentativaLogin(chave));
    }
    public synchronized TentativaLoginIP consultarIp(String ip) {
        String chave = Identificadores.validarIp(ip);
        return ips.getOrDefault(chave, new TentativaLoginIP(chave));
    }

    public synchronized void limpar(Instant agora) {
        nomes.values().removeIf(t -> t.podeDescartar(agora, porUsuario));
        ips.values().removeIf(t -> t.podeDescartar(agora, porIp));
    }
    public synchronized int quantidadeNomes() { return nomes.size(); }
    public synchronized int quantidadeIps() { return ips.size(); }
}
