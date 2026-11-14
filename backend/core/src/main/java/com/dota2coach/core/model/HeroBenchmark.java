package com.dota2coach.core.model;

import com.dota2coach.core.enums.BenchmarkMetric;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Distribuição de métricas por partida de um herói em partidas públicas (todas as faixas), em percentis.
 *
 * @param byMetric pontos (percentil, valor) de cada métrica, em ordem crescente de percentil
 */
public record HeroBenchmark(int heroId, Map<BenchmarkMetric, List<Point>> byMetric) {

    public HeroBenchmark {
        byMetric = byMetric.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .sorted(Comparator.comparingDouble(Point::percentile))
                        .toList()));
    }

    /** Um ponto da curva: {@code percentile} (0 a 1) das partidas têm valor menor ou igual a {@code value}. */
    public record Point(double percentile, double value) {}

    /**
     * Em que percentil (0 a 1) o valor cai, interpolando entre os pontos. Abaixo do primeiro ponto devolve 0 e acima
     * do último devolve 1, porque a OpenDota só publica de 10% a 99%.
     *
     * @return vazio se não há dados da métrica
     */
    public OptionalDouble percentileOf(BenchmarkMetric metric, double value) {
        List<Point> points = byMetric.getOrDefault(metric, List.of());
        if (points.isEmpty()) {
            return OptionalDouble.empty();
        }
        if (value < points.get(0).value()) {
            return OptionalDouble.of(0.0);
        }
        Point last = points.get(points.size() - 1);
        if (value > last.value()) {
            return OptionalDouble.of(1.0);
        }
        for (int i = 1; i < points.size(); i++) {
            Point lo = points.get(i - 1);
            Point hi = points.get(i);
            if (value <= hi.value()) {
                double span = hi.value() - lo.value();
                double t = span <= 0 ? 0.0 : (value - lo.value()) / span;
                return OptionalDouble.of(lo.percentile() + t * (hi.percentile() - lo.percentile()));
            }
        }
        return OptionalDouble.of(last.percentile());
    }
}
