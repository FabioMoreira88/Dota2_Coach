package com.dota2coach.analysis.support;

import com.dota2coach.core.enums.GameMode;
import com.dota2coach.core.enums.GamePhase;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.RankBracket;
import com.dota2coach.core.enums.Side;
import com.dota2coach.core.model.Hero;
import com.dota2coach.core.model.HeroItemPopularity;
import com.dota2coach.core.model.HeroMeta;
import com.dota2coach.core.model.HeroMeta.PickWin;
import com.dota2coach.core.model.Item;
import com.dota2coach.core.model.MatchRecord;
import com.dota2coach.core.model.PlayerSummary;
import com.dota2coach.core.model.ProfileSnapshot;
import com.dota2coach.core.model.SteamId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Dados de teste do Raio-X. */
public final class Snapshots {

    public static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");
    public static final int SNIPER = 35;
    public static final int OGRE = 84;
    public static final int AXE = 2;

    public static final int TREADS = 63;
    public static final int MAELSTROM = 166;
    public static final int DRAGON_LANCE = 236;
    public static final int BKB = 116;
    public static final int BLINK = 1;
    public static final int TANGO = 44;
    public static final int DESOLATOR = 168;
    public static final int HYPERSTONE = 64;

    private Snapshots() {}

    public static Map<Integer, Hero> heroes() {
        return Map.of(
                SNIPER, new Hero(SNIPER, "Sniper", "agi", List.of("Carry", "Nuker")),
                OGRE, new Hero(OGRE, "Ogre Magi", "str", List.of("Support", "Nuker", "Disabler")),
                AXE, new Hero(AXE, "Axe", "str", List.of("Initiator", "Durable")));
    }

    public static Map<Integer, Item> items() {
        return Map.of(
                TREADS, new Item(TREADS, "power_treads", "Power Treads", 1400, true),
                MAELSTROM, new Item(MAELSTROM, "maelstrom", "Maelstrom", 2950, true),
                DRAGON_LANCE, new Item(DRAGON_LANCE, "dragon_lance", "Dragon Lance", 1900, true),
                BKB, new Item(BKB, "black_king_bar", "Black King Bar", 4050, true),
                BLINK, new Item(BLINK, "blink", "Blink Dagger", 2250, false),
                TANGO, new Item(TANGO, "tango", "Tango", 90, false),
                DESOLATOR, new Item(DESOLATOR, "desolator", "Desolator", 3500, true),
                HYPERSTONE, new Item(HYPERSTONE, "hyperstone", "Hyperstone", 2000, false));
    }

    public static Map<Integer, HeroMeta> meta() {
        return Map.of(
                SNIPER, heroMeta(SNIPER, 1000, 540),
                OGRE, heroMeta(OGRE, 600, 270),
                AXE, heroMeta(AXE, 400, 200));
    }

    private static HeroMeta heroMeta(int heroId, int legendPicks, int legendWins) {
        return new HeroMeta(heroId, Map.of(
                RankBracket.LEGEND, new PickWin(legendPicks, legendWins),
                RankBracket.HERALD, new PickWin(100, 10)), PickWin.NONE);
    }

    public static Map<Integer, HeroItemPopularity> popularity() {
        return Map.of(SNIPER, new HeroItemPopularity(SNIPER, Map.of(
                GamePhase.START, Map.of(TANGO, 900),
                GamePhase.EARLY, Map.of(TREADS, 800, TANGO, 500),
                GamePhase.MID, Map.of(MAELSTROM, 600, DRAGON_LANCE, 500),
                GamePhase.LATE, Map.of(HYPERSTONE, 500, BKB, 400, DESOLATOR, 300))));
    }

    public static PlayerSummary player(Integer rankTier) {
        return new PlayerSummary(SteamId.fromAccountId(34839344L), "Heisenberg", null, rankTier, 0, 0, List.of());
    }

    public static ProfileSnapshot snapshot(Integer rankTier, List<MatchRecord> matches) {
        return new ProfileSnapshot(player(rankTier), matches, heroes(), items(), meta(), popularity(), NOW);
    }

    /** Partida padrão de 40 minutos, {@code daysAgo} dias antes de {@link #NOW}. */
    public static MatchRecord match(long id, int heroId, boolean won, double daysAgo) {
        return match(id, heroId, won, daysAgo, Lane.UNKNOWN, List.of());
    }

    public static MatchRecord match(long id, int heroId, boolean won, double daysAgo, Lane lane, List<Integer> items) {
        return new MatchRecord(id, NOW.minusSeconds((long) (daysAgo * 86400)), Duration.ofMinutes(40), heroId,
                Side.RADIANT, won, GameMode.ALL_PICK, true, 1, false, 6, 4, 10, 500, 600, 200, 5, 24000, 2000, 0,
                lane, items, 0, lane != Lane.UNKNOWN);
    }
}
