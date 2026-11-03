package entities;

import java.time.Instant;

public abstract class RegistroBase implements Auditavel {
    private final Instant criadoEm;

    protected RegistroBase() {
        this.criadoEm = Instant.now();
    }

    public final Instant getCriadoEm() {
        return criadoEm;
    }
}
