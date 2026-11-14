package com.dota2coach.analysis.support;

import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.DamageType;
import com.dota2coach.core.enums.GameMode;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Side;
import com.dota2coach.core.model.AnalyzedMatch;
import com.dota2coach.core.model.HeroDurations;
import com.dota2coach.core.model.HeroItemTimings;
import com.dota2coach.core.model.HeroMatchups;
import com.dota2coach.core.model.MatchDetail;
import com.dota2coach.core.model.MatchPlayer;
import com.dota2coach.core.model.MatchSnapshot;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** Partidas de teste para a análise de partida escolhida. */
public final class Matches {

    public static final long ACCOUNT = 34839344L;
    public static final long MATCH_ID = 9083198111L;
    /** Pickado pelo Dire, mas o jogador trocou de herói: não está entre os jogadores. */
    static final int JUGGERNAUT = 8;

    private Matches() {}

    /**
     * Sniper (a conta) no meio do Radiant contra Axe no meio do Dire; Ogre Magi no Radiant em outra rota.
     * O Radiant perde. 40 minutos.
     */
    public static MatchSnapshot sniperMid(boolean parsed) {
        MatchPlayer me = new MatchPlayer(0, ACCOUNT, Snapshots.SNIPER, Side.RADIANT,
                parsed ? Lane.MID_LANE : Lane.UNKNOWN, parsed ? 2 : null,
                8, 3, 6, 300, 15, 560, 650, 25000, 4000, 0, 21000, 25,
                parsed ? series(4) : List.of(), parsed ? series(1) : List.of(),
                parsed ? series(300) : List.of(), parsed ? series(320) : List.of(),
                parsed ? List.of(new MatchDetail.Purchase("tango", -80), new MatchDetail.Purchase("power_treads", 720),
                        new MatchDetail.Purchase("maelstrom", 1080), new MatchDetail.Purchase("power_treads", 1500),
                        new MatchDetail.Purchase("dragon_lance", 1700), new MatchDetail.Purchase("hurricane_pike", 1700))
                        : List.of(),
                parsed ? List.of(new MatchPlayer.Death(300, 15, Snapshots.AXE), new MatchPlayer.Death(1900, 45, null))
                        : List.of(),
                List.of(), parsed ? 1 : 0, parsed ? 2 : 0, parsed ? 72 : null,
                parsed ? Map.of(BenchmarkMetric.GOLD_PER_MIN, 0.66) : Map.of(), parsed ? 0.4 : null,
                parsed ? Map.of("null", 15000L, "sniper_shrapnel", 5000L, "sniper_assassinate", 5000L) : Map.of(),
                parsed ? Map.of("null", 3000L, "axe_counter_helix", 2000L) : Map.of(),
                parsed ? stats() : MatchPlayer.ReplayStats.EMPTY);
        MatchPlayer ally = new MatchPlayer(1, null, Snapshots.OGRE, Side.RADIANT, Lane.SAFE_LANE, 1,
                12, 9, 10, 40, 1, 250, 300, 8000, 0, 2000, 6000, 16,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 0, 0, null, Map.of(), null,
                parsed ? Map.of("ogre_magi_fireblast", 8000L) : Map.of(), Map.of());
        MatchPlayer rival = new MatchPlayer(128, 99L, Snapshots.AXE, Side.DIRE, Lane.MID_LANE, parsed ? 2 : null,
                12, 2, 9, 150, 3, 480, 600, 30000, 1000, 0, 24000, 26,
                parsed ? series(5) : List.of(), parsed ? series(0) : List.of(),
                parsed ? series(350) : List.of(), parsed ? series(340) : List.of(),
                List.of(), List.of(), List.of(), 0, 0, null, Map.of(), null,
                parsed ? Map.of("null", 10000L, "axe_counter_helix", 20000L) : Map.of(), Map.of());
        List<Integer> goldAdv = parsed ? IntStream.rangeClosed(0, 40).map(m -> 100 * m - 2000 * (m / 20)).boxed()
                .toList() : List.of();
        AnalyzedMatch match = new AnalyzedMatch(MATCH_ID, Snapshots.NOW, Duration.ofMinutes(40), false,
                GameMode.ALL_PICK, true, parsed, goldAdv,
                List.of(new AnalyzedMatch.DraftPick(1, Snapshots.AXE, Side.DIRE),
                        new AnalyzedMatch.DraftPick(2, Snapshots.OGRE, Side.RADIANT),
                        new AnalyzedMatch.DraftPick(3, JUGGERNAUT, Side.DIRE),
                        new AnalyzedMatch.DraftPick(4, Snapshots.SNIPER, Side.RADIANT)),
                List.of(me, ally, rival), parsed ? objectives() : List.of(), parsed ? teamfights() : List.of());
        return new MatchSnapshot(match, Snapshots.heroes(), Snapshots.items(), damageTypes(), durations(),
                new HeroMatchups(Snapshots.SNIPER, Map.of(Snapshots.AXE, new HeroMatchups.Record(1000, 470),
                        Snapshots.OGRE, new HeroMatchups.Record(5, 5))),
                parsed ? Snapshots.popularity() : Map.of(), parsed ? itemTimings() : Map.of(),
                Map.of("sniper_shrapnel", "Shrapnel"));
    }

    /** First blood do Dire, uma T1 de cada lado, Roshan do Dire e um barracks do Radiant caindo. */
    static List<AnalyzedMatch.Objective> objectives() {
        return List.of(
                new AnalyzedMatch.Objective(300, AnalyzedMatch.ObjectiveKind.FIRST_BLOOD, Side.DIRE, null),
                new AnalyzedMatch.Objective(700, AnalyzedMatch.ObjectiveKind.BUILDING, Side.RADIANT,
                        "npc_dota_badguys_tower1_mid"),
                new AnalyzedMatch.Objective(900, AnalyzedMatch.ObjectiveKind.BUILDING, Side.DIRE,
                        "npc_dota_goodguys_tower1_bot"),
                new AnalyzedMatch.Objective(1500, AnalyzedMatch.ObjectiveKind.ROSHAN, Side.DIRE, null),
                new AnalyzedMatch.Objective(2300, AnalyzedMatch.ObjectiveKind.BUILDING, Side.DIRE,
                        "npc_dota_goodguys_range_rax_mid"));
    }

    /**
     * Três lutas: uma pequena que o Radiant ganha (+300), uma em que o Sniper morre e o Radiant perde 1500, e uma
     * em que o Sniper não está e o Radiant perde 200.
     */
    static List<AnalyzedMatch.Teamfight> teamfights() {
        return List.of(
                new AnalyzedMatch.Teamfight(600, 640, List.of(
                        new AnalyzedMatch.FightPlayer(Snapshots.SNIPER, Side.RADIANT, 0, 300, 1200, 0, 0),
                        new AnalyzedMatch.FightPlayer(Snapshots.OGRE, Side.RADIANT, 1, -100, 300, 0, 0),
                        new AnalyzedMatch.FightPlayer(Snapshots.AXE, Side.DIRE, 2, -100, 500, 0, 0))),
                new AnalyzedMatch.Teamfight(1880, 1930, List.of(
                        new AnalyzedMatch.FightPlayer(Snapshots.SNIPER, Side.RADIANT, 1, -300, 800, 0, 0),
                        new AnalyzedMatch.FightPlayer(Snapshots.OGRE, Side.RADIANT, 1, -200, 200, 0, 1),
                        new AnalyzedMatch.FightPlayer(Snapshots.AXE, Side.DIRE, 0, 1000, 3000, 0, 0))),
                new AnalyzedMatch.Teamfight(2100, 2130, List.of(
                        new AnalyzedMatch.FightPlayer(Snapshots.SNIPER, Side.RADIANT, 0, 0, 0, 0, 0),
                        new AnalyzedMatch.FightPlayer(Snapshots.OGRE, Side.RADIANT, 1, -100, 100, 0, 0),
                        new AnalyzedMatch.FightPlayer(Snapshots.AXE, Side.DIRE, 1, 100, 600, 0, 0))));
    }

    /**
     * Sniper costuma fazer Power Treads entre 10 e 12 min e Maelstrom entre 12 e 15 min. Sem dados do Axe nem da
     * Hurricane Pike.
     */
    static Map<Integer, HeroItemTimings> itemTimings() {
        return Map.of(Snapshots.SNIPER, new HeroItemTimings(Snapshots.SNIPER, Map.of(
                "power_treads", List.of(new HeroItemTimings.Bin(600, 100, 50), new HeroItemTimings.Bin(720, 200, 110)),
                "maelstrom", List.of(new HeroItemTimings.Bin(450, 12, 9), new HeroItemTimings.Bin(600, 37, 20),
                        new HeroItemTimings.Bin(720, 97, 55), new HeroItemTimings.Bin(900, 286, 147),
                        new HeroItemTimings.Bin(1200, 186, 93), new HeroItemTimings.Bin(1500, 58, 28),
                        new HeroItemTimings.Bin(1800, 22, 8)))));
    }

    /**
     * Sniper recebeu 3000 do Axe e 1000 de creeps e torres; farm de 60% em creeps de rota; perdeu 500 de ouro
     * ao morrer e gastou 300 em buyback.
     */
    static MatchPlayer.ReplayStats stats() {
        return new MatchPlayer.ReplayStats(Map.of(Snapshots.AXE, 3000L), 1000,
                Map.of("sniper_shrapnel", 10L, "sniper_assassinate", 4L),
                Map.of("black_king_bar", 2L, "tpscroll", 3L, "power_treads", 0L), 3.5,
                Map.of(13, 6000L, 12, 2000L, 14, 2000L, 1, -500L, 2, -300L, 6, 400L),
                Map.of(2, 8000L, 1, 2000L), Map.of(5, 3L, 0, 1L), 2, 1, 0);
    }

    static Map<String, DamageType> damageTypes() {
        return Map.of("sniper_shrapnel", DamageType.MAGICAL, "sniper_assassinate", DamageType.MAGICAL,
                "ogre_magi_fireblast", DamageType.MAGICAL, "axe_counter_helix", DamageType.PURE);
    }

    /** Sniper perde força com o tempo; Axe ganha. Ogre só tem amostra entre 20 e 30 minutos. */
    static Map<Integer, HeroDurations> durations() {
        return Map.of(
                Snapshots.SNIPER, new HeroDurations(Snapshots.SNIPER, List.of(new HeroDurations.Bin(600, 100, 60),
                        new HeroDurations.Bin(1500, 200, 100), new HeroDurations.Bin(2100, 200, 90),
                        new HeroDurations.Bin(2700, 100, 40))),
                Snapshots.OGRE, new HeroDurations(Snapshots.OGRE, List.of(new HeroDurations.Bin(1500, 100, 50),
                        new HeroDurations.Bin(3300, 10, 10))),
                Snapshots.AXE, new HeroDurations(Snapshots.AXE, List.of(new HeroDurations.Bin(600, 100, 40),
                        new HeroDurations.Bin(1500, 100, 50), new HeroDurations.Bin(2700, 100, 60))));
    }

    /** Série por minuto que cresce {@code perMinute} por minuto até os 40 minutos. */
    private static List<Integer> series(int perMinute) {
        return IntStream.rangeClosed(0, 40).map(m -> m * perMinute).boxed().toList();
    }
}
