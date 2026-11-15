package com.dota2coach.analysis.evolution;

import static com.dota2coach.analysis.support.Snapshots.SNIPER;
import static com.dota2coach.analysis.support.Snapshots.match;
import static com.dota2coach.analysis.support.Snapshots.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.dota2coach.analysis.evolution.XRayEvolution.Change;
import com.dota2coach.analysis.evolution.XRayEvolution.Format;
import com.dota2coach.analysis.evolution.XRayEvolution.Trend;
import com.dota2coach.analysis.metric.ProfileXRay;
import com.dota2coach.analysis.metric.XRayCalculator;
import com.dota2coach.analysis.report.XRayMarkdownRenderer;
import com.dota2coach.core.enums.Position;
import com.dota2coach.core.model.MatchDetail;
import com.dota2coach.core.model.MatchRecord;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class XRayEvolutionCalculatorTest {

    /** 20 derrotas antigas; o atual tem mais 10 vitórias novas e last hits aos 10 min maiores. */
    private static ProfileXRay xRay(int newWins, int lastHitsAt10) {
        List<MatchRecord> matches = new ArrayList<>();
        Map<Long, MatchDetail> details = new HashMap<>();
        for (int i = 0; i < newWins; i++) {
            matches.add(match(100 + i, SNIPER, true, i * 0.1));
        }
        for (int i = 0; i < 20; i++) {
            matches.add(match(i + 1, SNIPER, false, 5 + i));
        }
        for (MatchRecord m : matches.subList(0, 3)) {
            details.put(m.matchId(), new MatchDetail(m.matchId(), SNIPER, lastHitsAt10, 2,
                    List.of(), List.of(new MatchDetail.Death(900, 30))));
        }
        return new XRayCalculator().calculate(snapshot(53, matches, details), Position.MID);
    }

    @Test
    void comparaRecorteFundamentosReplayEHerois() {
        XRayEvolution e = XRayEvolutionCalculator.compare(xRay(0, 40), xRay(10, 50));

        assertThat(e.newMatches()).isEqualTo(10);
        assertThat(e.previousPosition()).isEqualTo(Position.MID);
        assertThat(e.changes()).extracting(Change::group).containsSubsequence(
                "Últimas 100 partidas", "Fundamentos (últimas 20 partidas)", "Partidas com replay analisado",
                "Sniper (média do jogador)");
        assertThat(e.changes()).extracting(Change::label, Change::trend).contains(
                tuple("Taxa de vitória", Trend.IMPROVED),
                tuple("Ouro por minuto", Trend.SAME),
                tuple("Last hits aos 10 min", Trend.IMPROVED),
                tuple("Mortes de 10 a 20 min", Trend.SAME));
    }

    @Test
    void tendenciaRespeitaDirecaoETolerancia() {
        assertThat(XRayEvolutionCalculator.trend(40.0, 41.0, Format.DECIMAL, true)).isEqualTo(Trend.SAME);
        assertThat(XRayEvolutionCalculator.trend(40.0, 45.0, Format.DECIMAL, true)).isEqualTo(Trend.IMPROVED);
        assertThat(XRayEvolutionCalculator.trend(2.5, 2.0, Format.DECIMAL, false)).isEqualTo(Trend.IMPROVED);
        assertThat(XRayEvolutionCalculator.trend(0.50, 0.45, Format.PERCENT, true)).isEqualTo(Trend.WORSE);
        assertThat(XRayEvolutionCalculator.trend(0.50, 0.51, Format.PERCENT, true)).isEqualTo(Trend.SAME);
        assertThat(XRayEvolutionCalculator.trend(300.0, 305.0, Format.SECONDS, false)).isEqualTo(Trend.SAME);
        assertThat(XRayEvolutionCalculator.trend(null, 1.0, Format.DECIMAL, true)).isEqualTo(Trend.NO_DATA);
    }

    @Test
    void relatorioMostraAEvolucaoLogoDepoisDoCabecalho() {
        ProfileXRay current = xRay(10, 50);
        XRayEvolution e = XRayEvolutionCalculator.compare(xRay(0, 40), current);

        String md = new XRayMarkdownRenderer(ZoneId.of("America/Sao_Paulo")).render(current, e);

        assertThat(md).contains("## Evolução desde o último Raio-X",
                "10 partidas novas desde então; as outras 90 do recorte recente",
                "| Last hits aos 10 min | 40,0 | 50,0 | +10,0 | melhorou |",
                "| Taxa de vitória | 0,0% | 33,3% | +33,3 p.p. | melhorou |",
                "| Tempo morto por partida | 0:30 | 0:30 | +0:00 | igual |");
        assertThat(md.indexOf("## Evolução")).isLessThan(md.indexOf("## Visão geral"));
        assertThat(md).doesNotContain("A posição informada mudou");
    }
}
