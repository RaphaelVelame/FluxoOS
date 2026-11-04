package br.com.fluxoos.application.seguranca;

/** Contador secundário por IP, sujeito a falsos positivos em redes compartilhadas. */
public final class TentativaLoginIP extends ControleTentativas {
    private final String ip;
    public TentativaLoginIP(String ip) { this.ip = Identificadores.validarIp(ip); }
    public String getIp() { return ip; }
}
