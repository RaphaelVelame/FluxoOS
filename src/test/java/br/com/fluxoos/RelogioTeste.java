package br.com.fluxoos;

import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

public class RelogioTeste extends Clock {
    private final AtomicReference<Instant> agora = new AtomicReference<>(Instant.parse("2026-11-11T12:00:00Z"));
    public void avancar(Duration tempo) { agora.updateAndGet(a -> a.plus(tempo)); }
    @Override public Instant instant() { return agora.get(); }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zona) { return Clock.fixed(instant(), zona); }
}
