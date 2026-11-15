package com.dota2coach.analysis.evolution;

import static org.assertj.core.api.Assertions.assertThat;

import com.dota2coach.analysis.report.XRayMarkdownRenderer;
import com.dota2coach.analysis.metric.MatchAnalysisCalculatorTestSupport;
import com.dota2coach.core.enums.ScoreArea;
import com.dota2coach.core.model.MatchAreaScore;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AreaTrendCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");

    private static MatchAreaScore score(long matchId, int hoursAgo, ScoreArea area, int value) {
        return new MatchAreaScore(null, 1L, matchId, NOW.minusSeconds(hoursAgo * 3600L), area, value, null);
    }

    @Test
    void mediaDasUltimasDezContraAsDezAnterioresSemRepetirPartida() {
        List<MatchAreaScore> scores = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            scores.add(score(100 + i, i * 10, ScoreArea.VISION, i < 10 ? 6 : 4));
        }
        scores.add(score(100, 200, ScoreArea.VISION, 1));
        scores.add(score(999, 0, ScoreArea.VISION, 10));
        scores.add(score(100, 0, ScoreArea.LANE, 8));

        List<AreaTrend> trends = AreaTrendCalculator.trends(scores, 999L);

        assertThat(trends).extracting(AreaTrend::area).containsExactly(ScoreArea.LANE, ScoreArea.VISION);
        AreaTrend vision = trends.get(1);
        assertThat(vision.matches()).as("a nota antiga repetida da partida 100 e a partida excluída não contam")
                .isEqualTo(12);
        assertThat(vision.recentAverage()).isEqualTo(6.0);
        assertThat(vision.previousAverage()).isEqualTo(4.0);
        assertThat(vision.lastScores()).containsExactly(6, 6, 6, 6, 6);
        assertThat(trends.get(0).previousAverage()).isNull();
        assertThat(AreaTrendCalculator.trends(List.of(), null)).isEmpty();
    }

    @Test
    void raioXMostraATabela() {
        List<AreaTrend> trends = AreaTrendCalculator.trends(List.of(score(1, 1, ScoreArea.SURVIVAL, 5),
                score(2, 2, ScoreArea.SURVIVAL, 4)), null);

        String md = new XRayMarkdownRenderer(ZoneId.of("America/Sao_Paulo"))
                .render(MatchAnalysisCalculatorTestSupport.latestXRay(), null, List.of(), trends);

        assertThat(md).contains("## Notas por área das partidas revisadas", "| Sobrevivência | 2 | 4,5 | — | 5, 4 |");
    }
}
