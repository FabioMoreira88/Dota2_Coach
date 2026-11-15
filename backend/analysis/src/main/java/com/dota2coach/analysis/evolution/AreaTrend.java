package com.dota2coach.analysis.evolution;

import com.dota2coach.core.enums.ScoreArea;
import java.util.List;

/**
 * Notas de uma área nas partidas comentadas.
 *
 * @param matches partidas com nota nessa área
 * @param recentAverage média das últimas {@link AreaTrendCalculator#WINDOW} partidas
 * @param previousAverage média das {@link AreaTrendCalculator#WINDOW} partidas antes dessas, ou nulo sem elas
 * @param lastScores últimas notas, da mais recente para a mais antiga
 */
public record AreaTrend(ScoreArea area, int matches, double recentAverage, Double previousAverage,
        List<Integer> lastScores) {

    public AreaTrend {
        lastScores = List.copyOf(lastScores);
    }
}
