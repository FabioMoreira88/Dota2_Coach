package com.dota2coach.analysis.metric;

import static com.dota2coach.analysis.support.Snapshots.AXE;
import static com.dota2coach.analysis.support.Snapshots.BKB;
import static com.dota2coach.analysis.support.Snapshots.DRAGON_LANCE;
import static com.dota2coach.analysis.support.Snapshots.MAELSTROM;
import static com.dota2coach.analysis.support.Snapshots.OGRE;
import static com.dota2coach.analysis.support.Snapshots.SNIPER;
import static com.dota2coach.analysis.support.Snapshots.TANGO;
import static com.dota2coach.analysis.support.Snapshots.TREADS;
import static com.dota2coach.analysis.support.Snapshots.BLINK;
import static com.dota2coach.analysis.support.Snapshots.match;
import static com.dota2coach.analysis.support.Snapshots.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.dota2coach.analysis.metric.ProfileXRay.HeroItems;
import com.dota2coach.analysis.metric.ProfileXRay.HeroLine;
import com.dota2coach.analysis.metric.ProfileXRay.ItemUse;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Position;
import com.dota2coach.core.enums.RankBracket;
import com.dota2coach.core.model.MatchRecord;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class XRayCalculatorTest {

    private final XRayCalculator calculator = new XRayCalculator();

    /** 120 partidas, uma por dia, da mais recente para a mais antiga. */
    private static List<MatchRecord> history() {
        List<MatchRecord> matches = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            int hero = i % 4 == 3 ? OGRE : SNIPER;
            if (i == 5) {
                hero = AXE;
            }
            Lane lane = i < 10 ? (hero == OGRE ? Lane.OFF_LANE : Lane.SAFE_LANE) : Lane.UNKNOWN;
            List<Integer> items = hero == SNIPER ? List.of(TREADS, MAELSTROM, TANGO, BLINK) : List.of();
            matches.add(match(i + 1, hero, i % 2 == 0, i, lane, items));
        }
        return matches;
    }

    @Test
    void visaoGeralSeparaHistoricoERecorteRecente() {
        List<MatchRecord> shuffled = new ArrayList<>(history());
        java.util.Collections.reverse(shuffled);

        ProfileXRay x = calculator.calculate(snapshot(53, shuffled));

        assertThat(x.overview().allTime()).isEqualTo(new WinLoss(120, 60));
        assertThat(x.overview().recent().games()).isEqualTo(100);
        assertThat(x.overview().last30Days()).isEqualTo(31);
        assertThat(x.overview().lastMatch()).isEqualTo(com.dota2coach.analysis.support.Snapshots.NOW);
        assertThat(x.bracket()).isEqualTo(RankBracket.LEGEND);
    }

    @Test
    void heroisOrdenadosPorPartidasRecentesComMetaDaFaixa() {
        ProfileXRay x = calculator.calculate(snapshot(53, history()));

        assertThat(x.heroes()).extracting(HeroLine::name).containsExactly("Sniper", "Ogre Magi", "Axe");
        HeroLine sniper = x.heroes().get(0);
        assertThat(sniper.recent().games()).isEqualTo(74);
        assertThat(sniper.allTime().games()).isEqualTo(89);
        assertThat(sniper.kda()).isEqualTo(4.0);
        assertThat(sniper.bracketWinRate()).isEqualTo(0.54);
        // 2000 escolhas na faixa = 200 partidas; Sniper em 1000 delas
        assertThat(sniper.bracketPickRate()).isEqualTo(5.0);
        assertThat(x.heroPoolSize()).isEqualTo(2);
    }

    @Test
    void semMedalhaUsaTodasAsFaixas() {
        ProfileXRay x = calculator.calculate(snapshot(null, history()));

        assertThat(x.bracket()).isNull();
        assertThat(x.heroes().get(0).bracketWinRate()).isCloseTo(550.0 / 1100, within(1e-9));
    }

    @Test
    void rotasSoContamPartidasAnalisadas() {
        ProfileXRay x = calculator.calculate(snapshot(53, history()));

        assertThat(x.laneCoverage()).isEqualTo(0.10);
        assertThat(x.lanes()).extracting(l -> l.lane()).containsExactly(Lane.SAFE_LANE, Lane.OFF_LANE);
        assertThat(x.lanes().get(0).result().games()).as("Sniper e Axe na segura").isEqualTo(8);
    }

    @Test
    void posicaoInformadaVaiParaORaioX() {
        assertThat(calculator.calculate(snapshot(53, history()), Position.MID).mainPosition())
                .isEqualTo(Position.MID);
        assertThat(calculator.calculate(snapshot(53, history())).mainPosition()).isNull();
    }

    @Test
    void fundamentosComparamUltimasVinteComAsAnteriores() {
        ProfileXRay x = calculator.calculate(snapshot(53, history()));

        assertThat(x.fundamentals().latest().matches()).isEqualTo(20);
        assertThat(x.fundamentals().previous().matches()).isEqualTo(80);
        assertThat(x.fundamentals().latest().lastHitsPerMin()).isEqualTo(5.0);
        assertThat(x.fundamentals().latest().heroDamagePerMin()).isEqualTo(600.0);
    }

    @Test
    void itensComparamComOsPopularesIgnorandoItensBaratos() {
        ProfileXRay x = calculator.calculate(snapshot(53, history()));

        assertThat(x.items()).hasSize(1);
        HeroItems sniper = x.items().get(0);
        assertThat(sniper.matchesWithItems()).isEqualTo(50);
        assertThat(sniper.playerTop()).extracting(ItemUse::name)
                .as("Blink é básico, mas conta como item final")
                .containsExactly("Blink Dagger", "Power Treads", "Maelstrom");
        assertThat(sniper.playerTop()).extracting(ItemUse::meta).containsExactly(false, true, true);
        assertThat(sniper.adherence()).isCloseTo(2.0 / 3, within(1e-9));
        assertThat(sniper.metaTop()).doesNotContain("Tango", "Hyperstone");
        assertThat(sniper.missingMeta()).containsExactly("Dragon Lance", "Black King Bar", "Desolator");
    }

    @Test
    void heroiSemPartidasSuficientesComItensFicaForaDosItens() {
        List<MatchRecord> poucas = List.of(
                match(1, SNIPER, true, 0, Lane.UNKNOWN, List.of(BKB, DRAGON_LANCE)),
                match(2, SNIPER, true, 1));

        assertThat(calculator.calculate(snapshot(53, poucas)).items()).isEmpty();
    }

    @Test
    void parcelaDePartidasComHeroisQueVencemNaFaixa() {
        ProfileXRay x = calculator.calculate(snapshot(53, history()));

        // Sniper 54% e Axe 50% contam; Ogre 45% não
        assertThat(x.shareOnWinningMetaHeroes()).isEqualTo(0.75);
    }

    @Test
    void perfilSemPartidas() {
        ProfileXRay x = calculator.calculate(snapshot(53, List.of()));

        assertThat(x.overview().allTime().games()).isZero();
        assertThat(x.overview().firstMatch()).isNull();
        assertThat(x.heroes()).isEmpty();
        assertThat(x.laneCoverage()).isZero();
        assertThat(x.fundamentals().latest()).isEqualTo(ProfileXRay.StatBlock.EMPTY);
    }
}
