package com.dota2coach.analysis.metric;

import static com.dota2coach.analysis.support.Snapshots.OGRE;
import static com.dota2coach.analysis.support.Snapshots.SNIPER;
import static com.dota2coach.analysis.support.Snapshots.match;
import static com.dota2coach.analysis.support.Snapshots.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

import com.dota2coach.analysis.metric.ProfileXRay.DeathPhase;
import com.dota2coach.analysis.metric.ProfileXRay.ItemTiming;
import com.dota2coach.analysis.metric.ProfileXRay.MatchDetails;
import com.dota2coach.core.model.MatchDetail;
import com.dota2coach.core.model.MatchDetail.Death;
import com.dota2coach.core.model.MatchDetail.Purchase;
import com.dota2coach.core.model.MatchRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MatchDetailsCalculatorTest {

    /** Três partidas de Sniper e uma de Ogre analisadas, mais duas sem análise. */
    private static ProfileXRay xRay() {
        List<MatchRecord> matches = new ArrayList<>();
        Map<Long, MatchDetail> details = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            int hero = i == 3 ? OGRE : SNIPER;
            matches.add(match(i + 1, hero, true, i));
        }
        details.put(1L, new MatchDetail(1, SNIPER, 40, 5,
                List.of(new Purchase("tango", -60), new Purchase("power_treads", 600), new Purchase("maelstrom", 900),
                        new Purchase("power_treads", 2000), new Purchase("hyperstone", 1200)),
                List.of(new Death(120, 10), new Death(700, 20))));
        details.put(2L, new MatchDetail(2, SNIPER, 50, 3,
                List.of(new Purchase("power_treads", 720), new Purchase("black_king_bar", 1500)),
                List.of(new Death(1100, 30))));
        details.put(3L, new MatchDetail(3, SNIPER, null, null,
                List.of(new Purchase("power_treads", 480), new Purchase("maelstrom", 1100)),
                List.of(new Death(2000, 60))));
        details.put(4L, new MatchDetail(4, OGRE, 10, 0, List.of(), List.of()));
        return new XRayCalculator().calculate(snapshot(53, matches, details));
    }

    @Test
    void faseDeRotasIgnoraPartidasQueAcabaramAntesDos10Minutos() {
        MatchDetails d = xRay().details();

        assertThat(d.analyzedMatches()).isEqualTo(4);
        assertThat(d.lastHitsAt10()).isEqualTo(100.0 / 3);
        assertThat(d.deniesAt10()).isEqualTo(8.0 / 3);
        assertThat(d.laningByHero()).as("Sniper tem só 2 partidas com 10 minutos; Ogre só 1").isEmpty();
    }

    @Test
    void mortesPorFase() {
        MatchDetails d = xRay().details();

        assertThat(d.deathsByPhase()).extracting(DeathPhase::label)
                .containsExactly("0 a 10 min", "10 a 20 min", "20 a 30 min", "30 min ou mais");
        assertThat(d.deathsByPhase()).extracting(DeathPhase::share).containsExactly(0.25, 0.5, 0.0, 0.25);
        assertThat(d.deathsByPhase().get(1).deathsPerMatch()).isEqualTo(0.5);
        assertThat(d.secondsDeadPerMatch()).isEqualTo(30.0);
    }

    @Test
    void tempoDosItensUsaAPrimeiraCompraESoItensPrincipais() {
        List<ItemTiming> timings = xRay().details().itemTimings();

        assertThat(timings).extracting(ItemTiming::itemName)
                .as("Tango é barato, Hyperstone é componente, BKB só em 1 de 3 partidas")
                .containsExactly("Power Treads", "Maelstrom");
        assertThat(timings.get(0).matches()).isEqualTo(3);
        assertThat(timings.get(0).medianMinute()).isEqualTo(10.0);
        assertThat(timings.get(1).medianMinute()).isEqualTo(1000 / 60.0);
    }

    @Test
    void semPartidasAnalisadas() {
        assertThat(new XRayCalculator().calculate(snapshot(53, List.of(match(1, SNIPER, true, 0)))).details())
                .isEqualTo(MatchDetails.EMPTY);
    }
}
