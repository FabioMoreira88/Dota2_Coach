package com.dota2coach.analysis.support;

import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.GameMode;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Side;
import com.dota2coach.core.model.AnalyzedMatch;
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
                parsed ? Map.of(BenchmarkMetric.GOLD_PER_MIN, 0.66) : Map.of(), parsed ? 0.4 : null);
        MatchPlayer ally = new MatchPlayer(1, null, Snapshots.OGRE, Side.RADIANT, Lane.SAFE_LANE, 1,
                12, 9, 10, 40, 1, 250, 300, 8000, 0, 2000, 6000, 16,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 0, 0, null, Map.of(), null);
        MatchPlayer rival = new MatchPlayer(128, 99L, Snapshots.AXE, Side.DIRE, Lane.MID_LANE, parsed ? 2 : null,
                12, 2, 9, 150, 3, 480, 600, 30000, 1000, 0, 24000, 26,
                parsed ? series(5) : List.of(), parsed ? series(0) : List.of(),
                parsed ? series(350) : List.of(), parsed ? series(340) : List.of(),
                List.of(), List.of(), List.of(), 0, 0, null, Map.of(), null);
        List<Integer> goldAdv = parsed ? IntStream.rangeClosed(0, 40).map(m -> 100 * m - 2000 * (m / 20)).boxed()
                .toList() : List.of();
        AnalyzedMatch match = new AnalyzedMatch(MATCH_ID, Snapshots.NOW, Duration.ofMinutes(40), false,
                GameMode.ALL_PICK, true, parsed, goldAdv, List.of(me, ally, rival));
        return new MatchSnapshot(match, Snapshots.heroes(), Snapshots.items());
    }

    /** Série por minuto que cresce {@code perMinute} por minuto até os 40 minutos. */
    private static List<Integer> series(int perMinute) {
        return IntStream.rangeClosed(0, 40).map(m -> m * perMinute).boxed().toList();
    }
}
