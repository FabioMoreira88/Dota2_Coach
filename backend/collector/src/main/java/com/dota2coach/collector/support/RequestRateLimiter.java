package com.dota2coach.collector.support;

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * Limita chamadas a um número por minuto, espaçando-as igualmente.
 *
 * <p>Seguro para várias threads: cada chamada reserva o próximo horário livre e espera até ele.
 */
public class RequestRateLimiter {

    private final long intervalNanos;
    private final LongSupplier nanoClock;
    private final Sleeper sleeper;
    private long nextFreeNanos;

    public RequestRateLimiter(int requestsPerMinute, LongSupplier nanoClock, Sleeper sleeper) {
        if (requestsPerMinute <= 0) {
            throw new IllegalArgumentException("requestsPerMinute deve ser positivo");
        }
        this.intervalNanos = Duration.ofMinutes(1).toNanos() / requestsPerMinute;
        this.nanoClock = nanoClock;
        this.sleeper = sleeper;
        this.nextFreeNanos = nanoClock.getAsLong();
    }

    public RequestRateLimiter(int requestsPerMinute) {
        this(requestsPerMinute, System::nanoTime, Sleeper.SYSTEM);
    }

    /** Bloqueia até a chamada poder ser feita sem passar do limite. */
    public void acquire() throws InterruptedException {
        long waitNanos;
        synchronized (this) {
            long now = nanoClock.getAsLong();
            long slot = Math.max(now, nextFreeNanos);
            nextFreeNanos = slot + intervalNanos;
            waitNanos = slot - now;
        }
        if (waitNanos > 0) {
            sleeper.sleep(Duration.ofNanos(waitNanos));
        }
    }
}
