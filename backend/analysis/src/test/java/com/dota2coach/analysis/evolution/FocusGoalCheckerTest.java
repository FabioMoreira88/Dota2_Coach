package com.dota2coach.analysis.evolution;

import static com.dota2coach.analysis.support.Matches.ACCOUNT;
import static com.dota2coach.analysis.support.Matches.sniperMid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.dota2coach.analysis.evolution.FocusGoalCheck.Status;
import com.dota2coach.analysis.metric.MatchAnalysis;
import com.dota2coach.analysis.metric.MatchAnalysisCalculator;
import com.dota2coach.analysis.metric.MatchAnalysisCalculatorTestSupport;
import com.dota2coach.analysis.metric.ProfileXRay;
import com.dota2coach.analysis.report.MatchAnalysisMarkdownRenderer;
import com.dota2coach.analysis.report.XRayMarkdownRenderer;
import com.dota2coach.core.enums.FocusMetric;
import com.dota2coach.core.enums.Position;
import com.dota2coach.core.model.FocusGoal;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class FocusGoalCheckerTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private static FocusGoal goal(int ordinal, FocusMetric metric, String hero, Double baseline, double target) {
        return new FocusGoal(null, ordinal, "Foco " + ordinal, metric, hero, baseline, target);
    }

    private static MatchAnalysis match() {
        return new MatchAnalysisCalculator().analyze(sniperMid(true), ACCOUNT, Position.MID, null);
    }

    @Test
    void partidaAtingeOuNaoEMetaDeOutroHeroiNaoSeAplica() {
        List<FocusGoalCheck> checks = FocusGoalChecker.checkMatch(match(), List.of(
                goal(1, FocusMetric.GOLD_PER_MIN, null, 500.0, 550),
                goal(2, FocusMetric.DEATHS_PER_MATCH, null, 5.0, 2),
                goal(3, FocusMetric.GOLD_PER_MIN, "Ogre Magi", 500.0, 600),
                goal(4, FocusMetric.DEATHS_0_10, null, 1.0, 0),
                goal(5, FocusMetric.WIN_RATE, null, 0.5, 0.55)));

        assertThat(checks).extracting(FocusGoalCheck::ordinal, FocusGoalCheck::current, FocusGoalCheck::status)
                .containsExactly(
                        tuple(1, 560.0, Status.REACHED),
                        tuple(2, 3.0, Status.MISSED),
                        tuple(3, null, Status.NOT_APPLICABLE),
                        tuple(4, 1.0, Status.MISSED),
                        tuple(5, null, Status.NOT_APPLICABLE));
    }

    @Test
    void raioXDizSeAtingiuAvancouFicouIgualOuPiorou() {
        ProfileXRay x = MatchAnalysisCalculatorTestSupport.latestXRay();
        double gpm = x.fundamentals().latest().goldPerMin();

        List<FocusGoalCheck> checks = FocusGoalChecker.checkXRay(x, List.of(
                goal(1, FocusMetric.GOLD_PER_MIN, null, gpm - 100, gpm),
                goal(2, FocusMetric.GOLD_PER_MIN, null, gpm - 100, gpm + 100),
                goal(3, FocusMetric.GOLD_PER_MIN, null, gpm, gpm + 100),
                goal(4, FocusMetric.GOLD_PER_MIN, null, gpm + 100, gpm + 200),
                goal(5, FocusMetric.GOLD_PER_MIN, "Herói que não está no Raio-X", gpm, gpm + 100)));

        assertThat(checks).extracting(FocusGoalCheck::status).containsExactly(
                Status.REACHED, Status.ADVANCED, Status.SAME, Status.WORSE, Status.NO_DATA);
    }

    @Test
    void tabelasNosRelatorios() {
        MatchAnalysis a = match();
        List<FocusGoalCheck> matchChecks = FocusGoalChecker.checkMatch(a,
                List.of(goal(1, FocusMetric.DEATHS_PER_MATCH, null, 5.0, 2)));
        String matchMd = new MatchAnalysisMarkdownRenderer(ZONE).render(a, matchChecks);

        assertThat(matchMd).contains("## Seus focos nesta partida",
                "| 1. Foco 1 | Mortes por partida | ≤ 2,0 | 3,0 | não atingiu |");
        assertThat(new MatchAnalysisMarkdownRenderer(ZONE).render(a)).doesNotContain("Seus focos nesta partida");

        ProfileXRay x = MatchAnalysisCalculatorTestSupport.latestXRay();
        List<FocusGoalCheck> xChecks = FocusGoalChecker.checkXRay(x,
                List.of(goal(1, FocusMetric.SECONDS_DEAD, null, 400.0, 300)));
        String xMd = new XRayMarkdownRenderer(ZONE).render(x, null, xChecks);

        assertThat(xMd).contains("## Metas do último diagnóstico",
                "| 1. Foco 1 | Tempo morto por partida (s) | 6:40 | ≤ 5:00 |");
    }
}
