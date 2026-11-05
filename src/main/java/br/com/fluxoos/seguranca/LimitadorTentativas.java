package br.com.fluxoos.seguranca;

import br.com.fluxoos.seguranca.PoliticaBloqueio;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Em memória, por JVM. Em produção, transferir para armazenamento distribuído atômico. */
public final class LimitadorTentativas {
    private final Map<String, TentativaLogin> nomes = new ConcurrentHashMap<>();
    private final Map<String, TentativaLoginIP> ips = new ConcurrentHashMap<>();
    private final PoliticaBloqueio porUsuario;
    private final PoliticaBloqueio porIp;

    public LimitadorTentativas(PoliticaBloqueio porUsuario, PoliticaBloqueio porIp) {
        this.porUsuario = java.util.Objects.requireNonNull(porUsuario);
        this.porIp = java.util.Objects.requireNonNull(porIp);
    }

    public synchronized boolean bloqueado(String nome, String ip, Instant agora) {
        return nome(nome).estaBloqueado(agora, porUsuario)
                || ip(ip).estaBloqueado(agora, porIp);
    }
    public synchronized void falha(String nome, String ip, Instant agora) {
        nome(nome).registrarFalha(agora, porUsuario);
        ip(ip).registrarFalha(agora, porIp);
    }
    public synchronized void sucesso(String nome) { nome(nome).registrarSucesso(); }
    public synchronized TentativaLogin consultarNome(String nome) { return nome(nome); }
    public synchronized TentativaLoginIP consultarIp(String ip) { return ip(ip); }

    private TentativaLogin nome(String nome) {
        String chave = Identificadores.normalizarUsuario(nome);
        return nomes.computeIfAbsent(chave, TentativaLogin::new);
    }
    private TentativaLoginIP ip(String ip) {
        String chave = Identificadores.validarIp(ip);
        return ips.computeIfAbsent(chave, TentativaLoginIP::new);
    }
}
