package com.dota2coach.analysis.evolution;

import com.dota2coach.core.enums.ScoreArea;
import com.dota2coach.core.model.MatchAreaScore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tendência das notas por área: média das últimas partidas comentadas contra as anteriores. */
public final class AreaTrendCalculator {

    public static final int WINDOW = 10;
    static final int LAST_SCORES = 5;

    private AreaTrendCalculator() {}

    /**
     * @param scores notas da conta, em qualquer ordem
     * @param excludeMatchId partida que não entra (a que está sendo revisada), ou nulo
     * @return uma linha por área com nota, na ordem das áreas; vazio sem notas
     */
    public static List<AreaTrend> trends(List<MatchAreaScore> scores, Long excludeMatchId) {
        Map<ScoreArea, Map<Long, Integer>> byArea = new LinkedHashMap<>();
        scores.stream()
                .filter(s -> excludeMatchId == null || s.getMatchId() != excludeMatchId)
                .sorted(Comparator.comparing(MatchAreaScore::getScoredAt).reversed())
                .forEach(s -> byArea.computeIfAbsent(s.getArea(), a -> new LinkedHashMap<>())
                        .putIfAbsent(s.getMatchId(), s.getScore()));
        List<AreaTrend> trends = new ArrayList<>();
        for (ScoreArea area : ScoreArea.values()) {
            Map<Long, Integer> byMatch = byArea.get(area);
            if (byMatch == null || byMatch.isEmpty()) {
                continue;
            }
            List<Integer> values = new ArrayList<>(byMatch.values());
            List<Integer> recent = values.subList(0, Math.min(WINDOW, values.size()));
            List<Integer> previous = values.subList(recent.size(), Math.min(2 * WINDOW, values.size()));
            trends.add(new AreaTrend(area, values.size(), average(recent),
                    previous.isEmpty() ? null : average(previous),
                    values.subList(0, Math.min(LAST_SCORES, values.size()))));
        }
        return trends;
    }

    private static double average(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).average().orElse(0);
    }
}
