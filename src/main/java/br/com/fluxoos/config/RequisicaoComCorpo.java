package br.com.fluxoos.config;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Reapresenta o corpo limitado para os controllers MVC sincronos, sem reler a rede. */
final class RequisicaoComCorpo extends HttpServletRequestWrapper {
    private final byte[] corpo;

    RequisicaoComCorpo(HttpServletRequest requisicao, byte[] corpo) {
        super(requisicao);
        this.corpo = corpo;
    }

    @Override public int getContentLength() { return corpo.length; }
    @Override public long getContentLengthLong() { return corpo.length; }

    @Override public ServletInputStream getInputStream() {
        var dados = new ByteArrayInputStream(corpo);
        return new ServletInputStream() {
            @Override public int read() { return dados.read(); }
            @Override public int read(byte[] b, int inicio, int tamanho) { return dados.read(b,inicio,tamanho); }
            @Override public boolean isFinished() { return dados.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener listener) {
                throw new IllegalStateException("A API MVC usa leitura sincrona do corpo.");
            }
        };
    }

    @Override public BufferedReader getReader() throws IOException {
        String encoding = getCharacterEncoding();
        return new BufferedReader(new InputStreamReader(getInputStream(),
                encoding == null ? StandardCharsets.UTF_8.name() : encoding));
    }
}
