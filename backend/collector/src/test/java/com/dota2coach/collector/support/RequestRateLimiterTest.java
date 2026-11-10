package com.dota2coach.collector.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RequestRateLimiterTest {

    @Test
    void espacaAsChamadasPeloIntervaloDoLimite() throws InterruptedException {
        AtomicLong now = new AtomicLong(0);
        List<Duration> waits = new ArrayList<>();
        RequestRateLimiter limiter = new RequestRateLimiter(60, now::get, d -> {
            waits.add(d);
            now.addAndGet(d.toNanos());
        });

        limiter.acquire();
        limiter.acquire();
        limiter.acquire();

        assertThat(waits).containsExactly(Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    @Test
    void naoEsperaQuandoJaPassouOIntervalo() throws InterruptedException {
        AtomicLong now = new AtomicLong(0);
        List<Duration> waits = new ArrayList<>();
        RequestRateLimiter limiter = new RequestRateLimiter(60, now::get, waits::add);

        limiter.acquire();
        now.addAndGet(Duration.ofSeconds(5).toNanos());
        limiter.acquire();

        assertThat(waits).isEmpty();
    }
}
