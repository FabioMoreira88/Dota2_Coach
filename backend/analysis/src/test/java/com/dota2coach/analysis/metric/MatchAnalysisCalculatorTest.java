package com.dota2coach.analysis.metric;

import static com.dota2coach.analysis.support.Matches.ACCOUNT;
import static com.dota2coach.analysis.support.Matches.sniperMid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dota2coach.analysis.metric.MatchAnalysis.DeathLine;
import com.dota2coach.analysis.metric.MatchAnalysis.DeathPhaseLine;
import com.dota2coach.analysis.metric.MatchAnalysis.ItemLine;
import com.dota2coach.analysis.metric.MatchAnalysis.LaneOpponent;
import com.dota2coach.analysis.metric.MatchAnalysis.TeamGold;
import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Position;
import org.junit.jupiter.api.Test;

class MatchAnalysisCalculatorTest {

    private final MatchAnalysisCalculator calculator = new MatchAnalysisCalculator();

    @Test
    void comparaComOAdversarioDeRotaEComOPadraoDoJogador() {
        MatchAnalysis a = calculator.analyze(sniperMid(true), ACCOUNT, Position.MID, MatchAnalysisCalculatorTestSupport.latestXRay());

        assertThat(a.won()).isFalse();
        assertThat(a.heroName()).isEqualTo("Sniper");
        assertThat(a.lane()).isEqualTo(Lane.MID_LANE);
        assertThat(a.killParticipation()).as("(8 + 6) / (8 + 12)").isEqualTo(0.7);
        assertThat(a.percentiles()).containsEntry(BenchmarkMetric.GOLD_PER_MIN, 0.66);

        assertThat(a.laning().lastHitsAt10()).isEqualTo(40);
        assertThat(a.laning().goldAt10()).isEqualTo(3000);
        assertThat(a.laning().laneEfficiencyPct()).isEqualTo(72);
        assertThat(a.laning().opponents()).containsExactly(new LaneOpponent("Axe", 50, 0, 3500, 3400));

        assertThat(a.deaths()).containsExactly(
                new DeathLine(300, "0 a 10 min", "Axe", 15),
                new DeathLine(1900, "30 min ou mais", null, 45));
        assertThat(a.deathsByPhase()).extracting(DeathPhaseLine::deaths).containsExactly(1, 0, 0, 1);
        assertThat(a.deathsByPhase().get(0).usualPerMatch()).isEqualTo(1.0);

        assertThat(a.items()).as("primeira compra de cada item principal, sem consumíveis")
                .containsExactly(new ItemLine("Power Treads", 720, 10.0), new ItemLine("Maelstrom", 1080, null));
        assertThat(a.observersPlaced()).isEqualTo(1);
        assertThat(a.sentriesPlaced()).isEqualTo(2);
        assertThat(a.teamGold()).containsExactly(new TeamGold(10, 1000), new TeamGold(20, 0),
                new TeamGold(30, 1000), new TeamGold(40, 0));

        assertThat(a.usual().heroGames()).isEqualTo(3);
        assertThat(a.usual().lastHitsAt10()).isEqualTo(41.0);
        assertThat(a.usual().lastHitsAt10ForHero()).isTrue();
        assertThat(a.usual().heroGoldPerMin()).isEqualTo(500.0);
    }

    @Test
    void semRaioXSalvoNaoComparaComOPadrao() {
        MatchAnalysis a = calculator.analyze(sniperMid(true), ACCOUNT, null, null);

        assertThat(a.usual()).isNull();
        assertThat(a.position()).isNull();
        assertThat(a.deathsByPhase()).extracting(DeathPhaseLine::usualPerMatch).containsOnlyNulls();
        assertThat(a.items()).extracting(ItemLine::usualMinute).containsOnlyNulls();
    }

    @Test
    void semReplayAnalisadoFicaSoComOResumo() {
        MatchAnalysis a = calculator.analyze(sniperMid(false), ACCOUNT, Position.MID, MatchAnalysisCalculatorTestSupport.latestXRay());

        assertThat(a.replayParsed()).isFalse();
        assertThat(a.line().kills()).isEqualTo(8);
        assertThat(a.laning()).isEqualTo(MatchAnalysis.Laning.EMPTY);
        assertThat(a.deaths()).isEmpty();
        assertThat(a.deathsByPhase()).isEmpty();
        assertThat(a.items()).isEmpty();
        assertThat(a.teamGold()).isEmpty();
    }

    @Test
    void contaForaDaPartidaEhErroClaro() {
        assertThatThrownBy(() -> calculator.analyze(sniperMid(true), 1L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A conta 1 não aparece na partida 9083198111");
    }
}
