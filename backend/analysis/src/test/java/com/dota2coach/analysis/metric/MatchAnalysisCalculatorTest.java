package com.dota2coach.analysis.metric;

import static com.dota2coach.analysis.support.Matches.ACCOUNT;
import static com.dota2coach.analysis.support.Matches.sniperMid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

import com.dota2coach.analysis.metric.MatchAnalysis.DeathLine;
import com.dota2coach.analysis.metric.MatchAnalysis.DeathPhaseLine;
import com.dota2coach.analysis.metric.MatchAnalysis.ItemLine;
import com.dota2coach.analysis.metric.MatchAnalysis.LaneOpponent;
import com.dota2coach.analysis.metric.MatchAnalysis.TeamGold;
import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Position;
import com.dota2coach.core.model.MatchSnapshot;
import java.util.List;
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

        assertThat(a.items())
                .as("primeira compra de cada item principal, sem consumíveis nem a Dragon Lance comprada no mesmo "
                        + "segundo da Hurricane Pike")
                .containsExactly(new ItemLine("Power Treads", 720, 10.0), new ItemLine("Maelstrom", 1080, null),
                        new ItemLine("Hurricane Pike", 1700, null));
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
    void contextoComDraftDanoMatchupsEForcaPorDuracao() {
        MatchAnalysis.Context c = calculator.analyze(sniperMid(true), ACCOUNT, Position.MID, null).context();

        assertThat(c.pickOrderKnown()).isTrue();
        assertThat(c.picks())
                .as("a sua posição é a informada; a dos outros, estimada pela rota e pelo farm; o Juggernaut, que "
                        + "não jogou, sai e a ordem é renumerada")
                .containsExactly(
                        new MatchAnalysis.Pick(1, "Axe", false, false, 2, "meio, maior patrimônio do time", false),
                        new MatchAnalysis.Pick(2, "Ogre Magi", true, false, 1,
                                "rota segura, menor patrimônio do time", true),
                        new MatchAnalysis.Pick(3, "Sniper", true, true, 2, null, false));
        assertThat(c.directOpponent()).isEqualTo("Axe");
        assertThat(c.yourTeamPickNumber()).isEqualTo(2);

        MatchAnalysis.TeamProfile yours = c.teams().get(0);
        assertThat(yours.yours()).isTrue();
        assertThat(yours.won()).isFalse();
        assertThat(yours.heroes()).containsExactly("Sniper", "Ogre Magi");
        assertThat(yours.damageDealt().total()).isEqualTo(33000);
        assertThat(yours.damageDealt().physical()).isCloseTo(15000 / 33000.0, within(1e-9));
        assertThat(yours.damageDealt().magical()).isCloseTo(18000 / 33000.0, within(1e-9));
        assertThat(yours.roles()).extracting(MatchAnalysis.RoleCount::role, MatchAnalysis.RoleCount::heroes)
                .contains(tuple("Carry", 1), tuple("Dano mágico em rajada (nuker)", 2), tuple("Suporte", 1));
        MatchAnalysis.TeamProfile enemy = c.teams().get(1);
        assertThat(enemy.won()).isTrue();
        assertThat(enemy.damageDealt().pure()).isCloseTo(2 / 3.0, within(1e-9));
        assertThat(c.damageReceivedByYou().physical()).isCloseTo(0.6, within(1e-9));
        assertThat(c.damageReceivedByYou().pure()).isCloseTo(0.4, within(1e-9));

        assertThat(c.matchups()).as("Ogre não é adversário; poucas partidas também ficariam de fora")
                .containsExactly(new MatchAnalysis.Matchup("Axe", true, true, 1000, 0.47));

        assertThat(c.power()).containsExactly(
                new MatchAnalysis.PhasePower("até 20 min", 0.6, 0.4),
                new MatchAnalysis.PhasePower("20 a 30 min", 0.5, 0.5),
                new MatchAnalysis.PhasePower("30 a 40 min", 0.45, null),
                new MatchAnalysis.PhasePower("40 a 50 min", 0.4, 0.6));
    }

    @Test
    void objetivosELutasDoPontoDeVistaDoSeuTime() {
        MatchAnalysis.Timeline t = calculator.analyze(sniperMid(true), ACCOUNT, Position.MID, null).timeline();

        assertThat(t.objectives()).containsExactly(
                new MatchAnalysis.ObjectiveLine(300, "tirou o first blood", false),
                new MatchAnalysis.ObjectiveLine(700, "derrubou a torre T1 do meio", true),
                new MatchAnalysis.ObjectiveLine(900, "derrubou a torre T1 de baixo", false),
                new MatchAnalysis.ObjectiveLine(1500, "matou o Roshan", false),
                new MatchAnalysis.ObjectiveLine(2300, "derrubou o barracks à distância do meio", false));
        assertThat(t.fightsTotal()).isEqualTo(3);
        assertThat(t.fightsWithYou()).isEqualTo(2);
        assertThat(t.fightsWonByGold()).isEqualTo(1);
        assertThat(t.biggestFights()).containsExactly(
                new MatchAnalysis.FightLine(600, 640, 300, List.of("Ogre Magi"), List.of("Axe", "Axe"), true, false,
                        0),
                new MatchAnalysis.FightLine(1880, 1930, -1500, List.of("Sniper", "Ogre Magi"), List.of(), true, true,
                        1),
                new MatchAnalysis.FightLine(2100, 2130, -200, List.of("Ogre Magi"), List.of("Axe"), false, false,
                        0));
    }

    @Test
    void ficamSoAsMaioresLutasEmOrdemDeTempo() {
        var fights = java.util.stream.IntStream.range(0, 7)
                .mapToObj(i -> new com.dota2coach.core.model.AnalyzedMatch.Teamfight(i * 100, i * 100 + 30, List.of(
                        new com.dota2coach.core.model.AnalyzedMatch.FightPlayer(
                                com.dota2coach.analysis.support.Snapshots.SNIPER,
                                com.dota2coach.core.enums.Side.RADIANT, 0, (i % 2 == 0 ? 1 : -1) * i * 100, 10, 0,
                                0))))
                .toList();
        var base = sniperMid(true);
        var match = base.match();
        var withFights = new com.dota2coach.core.model.AnalyzedMatch(match.matchId(), match.startTime(),
                match.duration(), match.radiantWin(), match.gameMode(), match.ranked(), true,
                match.radiantGoldAdvantage(), match.picks(), match.players(), match.objectives(), fights);
        var snapshot = new com.dota2coach.core.model.MatchSnapshot(withFights, base.heroes(), base.items(),
                base.damageTypes(), base.heroDurations(), base.playerMatchups(), base.itemPopularity(),
                base.itemTimings(), base.abilityNames());

        MatchAnalysis.Timeline t = calculator.analyze(snapshot, ACCOUNT, null, null).timeline();

        assertThat(t.fightsTotal()).isEqualTo(7);
        assertThat(t.biggestFights()).extracting(MatchAnalysis.FightLine::goldSwing)
                .as("as 5 maiores em valor absoluto, na ordem em que aconteceram")
                .containsExactly(200, -300, 400, -500, 600);
    }

    @Test
    void itensDosDezJogadoresComOsPopularesQueNaoFez() {
        List<MatchAnalysis.PlayerItems> items =
                calculator.analyze(sniperMid(true), ACCOUNT, Position.MID, null).playersItems();

        assertThat(items).extracting(MatchAnalysis.PlayerItems::heroName)
                .as("seu time primeiro, você no topo").containsExactly("Sniper", "Ogre Magi", "Axe");
        MatchAnalysis.PlayerItems me = items.get(0);
        assertThat(me.you()).isTrue();
        assertThat(me.items()).as("a Dragon Lance virou Hurricane Pike no mesmo segundo")
                .containsExactly(new MatchAnalysis.ItemTime("Power Treads", 720),
                        new MatchAnalysis.ItemTime("Maelstrom", 1080), new MatchAnalysis.ItemTime("Hurricane Pike", 1700));
        assertThat(me.popularNotBuilt())
                .as("Tango e Hyperstone não são itens principais; Treads, Maelstrom e Dragon Lance ele fez")
                .containsExactly("Black King Bar", "Desolator");
        assertThat(items.get(2).yourTeam()).isFalse();
        assertThat(items.get(2).popularKnown()).as("sem popularidade do Axe").isFalse();
    }

    @Test
    void ouroDoTimeACada10MinutosAteOFim() {
        var base = sniperMid(true).match();
        var longMatch = new com.dota2coach.core.model.AnalyzedMatch(base.matchId(), base.startTime(),
                java.time.Duration.ofMinutes(56), base.radiantWin(), base.gameMode(), base.ranked(), true,
                java.util.stream.IntStream.rangeClosed(0, 56).boxed().toList(), base.picks(), base.players(),
                List.of(), List.of());
        var me = longMatch.player(ACCOUNT).orElseThrow();

        assertThat(MatchAnalysisCalculator.teamGold(longMatch, me)).extracting(TeamGold::minute)
                .containsExactly(10, 20, 30, 40, 50);
    }

    @Test
    void pickForaDoComumSoParaCarryDe5ESuporteDe1() {
        List<String> spiritBreaker = List.of("Carry", "Initiator", "Disabler", "Durable", "Escape");

        assertThat(PositionEstimator.unusual(spiritBreaker, 4)).as("posição 4 varia muito").isFalse();
        assertThat(PositionEstimator.unusual(spiritBreaker, 5)).isTrue();
        assertThat(PositionEstimator.unusual(List.of("Support", "Nuker"), 1)).isTrue();
        assertThat(PositionEstimator.unusual(List.of("Carry", "Support"), 5)).isFalse();
    }

    @Test
    void semPosicaoInformadaNaoHaAdversarioDiretoNemPosicaoSua() {
        MatchAnalysis.Context c = calculator.analyze(sniperMid(true), ACCOUNT, null, null).context();

        assertThat(c.directOpponent()).isNull();
        assertThat(c.picks().get(2).position()).as("a posição do jogador nunca é estimada").isNull();
        assertThat(c.picks().get(0).position()).isEqualTo(2);
    }

    @Test
    void temposDeItemContraONormalDoHeroi() {
        List<MatchAnalysis.ItemTimingLine> lines =
                calculator.analyze(sniperMid(true), ACCOUNT, Position.MID, null).itemTimings();

        assertThat(lines).as("sem dados do Axe nem da Hurricane Pike").containsExactly(
                new MatchAnalysis.ItemTimingLine("Sniper", true, "Power Treads", 720, "10 a 12 min", "no normal",
                        110 / 200.0),
                new MatchAnalysis.ItemTimingLine("Sniper", true, "Maelstrom", 1080, "12 a 15 min",
                        "depois do normal", 93 / 186.0));
    }

    @Test
    void faixaDeTempoAntesDaPrimeiraEDepoisDaUltima() {
        var bins = List.of(new com.dota2coach.core.model.HeroItemTimings.Bin(450, 100, 60),
                new com.dota2coach.core.model.HeroItemTimings.Bin(600, 10, 5));

        assertThat(MatchItemTimingCalculator.line("Sniper", true, "Blink", 300, bins))
                .isEqualTo(new MatchAnalysis.ItemTimingLine("Sniper", true, "Blink", 300, "até 7,5 min", "no normal",
                        0.6));
        assertThat(MatchItemTimingCalculator.line("Sniper", true, "Blink", 2000, bins))
                .as("depois da última faixa não há taxa de vitória")
                .isEqualTo(new MatchAnalysis.ItemTimingLine("Sniper", true, "Blink", 2000, "até 7,5 min",
                        "depois do normal", null));
    }

    @Test
    void detalhesDoJogoDoJogador() {
        MatchAnalysis.PlayDetails p = calculator.analyze(sniperMid(true), ACCOUNT, Position.MID, null).play();

        assertThat(p.damageFromHeroes()).containsExactly(new MatchAnalysis.DamageFrom("Axe", 3000, 0.75));
        assertThat(p.damageFromOthersShare()).isEqualTo(0.25);
        assertThat(p.goldLostOnDeaths()).isEqualTo(500);
        assertThat(p.goldOnBuybacks()).isEqualTo(300);
        assertThat(p.goldSources()).as("morte, buyback e venda de item não são fonte de ouro").containsExactly(
                new MatchAnalysis.Share("Creeps de rota", 6000, 0.6), new MatchAnalysis.Share("Heróis", 2000, 0.2),
                new MatchAnalysis.Share("Neutros", 2000, 0.2));
        assertThat(p.xpSources()).containsExactly(new MatchAnalysis.Share("Creeps e neutros", 8000, 0.8),
                new MatchAnalysis.Share("Heróis", 2000, 0.2));
        assertThat(p.itemUses()).as("pelo nome do item quando ele é conhecido; sem os que não foram usados")
                .containsExactly(new MatchAnalysis.Use("tpscroll", 3), new MatchAnalysis.Use("Black King Bar", 2));
        assertThat(p.abilityUses()).containsExactly(new MatchAnalysis.Use("Shrapnel", 10),
                new MatchAnalysis.Use("sniper assassinate", 4));
        assertThat(p.stunSeconds()).isEqualTo(3.5);
        assertThat(p.runes()).containsExactly(new MatchAnalysis.Use("Bounty", 3),
                new MatchAnalysis.Use("Dano dobrado", 1));
        assertThat(p.campsStacked()).isEqualTo(2);
        assertThat(p.observerKills()).isEqualTo(1);
    }

    @Test
    void nomeDeHabilidadeSemNomeDeExibicaoFicaLegivel() {
        MatchSnapshot snapshot = sniperMid(true);

        assertThat(MatchPlayCalculator.abilityName(snapshot, "ability_capture")).isEqualTo("Captura de outpost");
        assertThat(MatchPlayCalculator.abilityName(snapshot, "abyssal_underlord_portal_warp"))
                .isEqualTo("Portal do Underlord");
        assertThat(MatchPlayCalculator.abilityName(snapshot, "some_new_thing")).isEqualTo("some new thing");
    }

    @Test
    void momentosParaReverNoReplay() {
        List<MatchAnalysis.ReplayMoment> moments =
                calculator.analyze(sniperMid(true), ACCOUNT, Position.MID, null).timeline().replayMoments();

        assertThat(moments).as("lutas, as suas primeiro, e a morte fora de luta; a morte das 31:40 foi em luta")
                .containsExactly(
                        new MatchAnalysis.ReplayMoment(270, 305, "você morreu fora de luta para Axe"),
                        new MatchAnalysis.ReplayMoment(600, 640, "luta com +300 de ouro para seu time; você participou"),
                        new MatchAnalysis.ReplayMoment(1880, 1930, "luta com -1500 de ouro para seu time; você morreu"),
                        new MatchAnalysis.ReplayMoment(2100, 2130,
                                "luta com -200 de ouro para seu time; você não estava"));
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
        assertThat(a.timeline()).isEqualTo(MatchAnalysis.Timeline.EMPTY);
        assertThat(a.playersItems()).isEmpty();
        assertThat(a.itemTimings()).isEmpty();
        assertThat(a.play()).isEqualTo(MatchAnalysis.PlayDetails.EMPTY);
        assertThat(a.context().directOpponent()).isNull();
    }

    @Test
    void contaForaDaPartidaEhErroClaro() {
        assertThatThrownBy(() -> calculator.analyze(sniperMid(true), 1L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A conta 1 não aparece na partida 9083198111");
    }
}
