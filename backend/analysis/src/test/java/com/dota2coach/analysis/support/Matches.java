package com.dota2coach.analysis.support;

import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.DamageType;
import com.dota2coach.core.enums.GameMode;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Side;
import com.dota2coach.core.model.AnalyzedMatch;
import com.dota2coach.core.model.HeroDurations;
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
                        new MatchDetail.Purchase("maelstrom", 1080), new MatchDetail.Purchase("power_treads", 1500))
                        : List.of(),
                parsed ? List.of(new MatchPlayer.Death(300, 15, Snapshots.AXE), new MatchPlayer.Death(1900, 45, null))
                        : List.of(),
                List.of(), parsed ? 1 : 0, parsed ? 2 : 0, parsed ? 72 : null,
                parsed ? Map.of(BenchmarkMetric.GOLD_PER_MIN, 0.66) : Map.of(), parsed ? 0.4 : null,
                parsed ? Map.of("null", 15000L, "sniper_shrapnel", 5000L, "sniper_assassinate", 5000L) : Map.of(),
                parsed ? Map.of("null", 3000L, "axe_counter_helix", 2000L) : Map.of());
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
                        new AnalyzedMatch.DraftPick(3, Snapshots.SNIPER, Side.RADIANT)),
                List.of(me, ally, rival));
        return new MatchSnapshot(match, Snapshots.heroes(), Snapshots.items(), damageTypes(), durations(),
                new HeroMatchups(Snapshots.SNIPER, Map.of(Snapshots.AXE, new HeroMatchups.Record(1000, 470),
                        Snapshots.OGRE, new HeroMatchups.Record(5, 5))));
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
