package com.dota2coach.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.dota2coach.core.enums.BenchmarkMetric;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HeroBenchmarkTest {

    private final HeroBenchmark benchmark = new HeroBenchmark(35, Map.of(BenchmarkMetric.GOLD_PER_MIN, List.of(
            new HeroBenchmark.Point(0.5, 450), new HeroBenchmark.Point(0.1, 300), new HeroBenchmark.Point(0.9, 700))));

    @Test
    void interpolaEntreOsPontos() {
        assertThat(benchmark.percentileOf(BenchmarkMetric.GOLD_PER_MIN, 450).getAsDouble()).isEqualTo(0.5);
        assertThat(benchmark.percentileOf(BenchmarkMetric.GOLD_PER_MIN, 375).getAsDouble()).isCloseTo(0.3, within(1e-9));
        assertThat(benchmark.percentileOf(BenchmarkMetric.GOLD_PER_MIN, 575).getAsDouble()).isCloseTo(0.7, within(1e-9));
    }

    @Test
    void foraDaFaixaPublicadaVaiParaOsExtremos() {
        assertThat(benchmark.percentileOf(BenchmarkMetric.GOLD_PER_MIN, 100).getAsDouble()).isZero();
        assertThat(benchmark.percentileOf(BenchmarkMetric.GOLD_PER_MIN, 900).getAsDouble()).isEqualTo(1.0);
    }

    @Test
    void semDadosDaMetricaDevolveVazio() {
        assertThat(benchmark.percentileOf(BenchmarkMetric.XP_PER_MIN, 500)).isEmpty();
    }
}
