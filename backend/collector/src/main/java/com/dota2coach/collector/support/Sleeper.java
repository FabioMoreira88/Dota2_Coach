package com.dota2coach.collector.support;

import java.time.Duration;

/** Abstração de espera, para que rate limit e retry sejam testáveis sem dormir de verdade. */
@FunctionalInterface
public interface Sleeper {

    void sleep(Duration duration) throws InterruptedException;

    Sleeper SYSTEM = duration -> Thread.sleep(duration.toMillis());
}
