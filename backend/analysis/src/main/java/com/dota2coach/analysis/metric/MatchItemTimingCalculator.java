package com.dota2coach.analysis.metric;

import com.dota2coach.analysis.metric.MatchAnalysis.ItemTimingLine;
import com.dota2coach.core.model.HeroItemTimings;
import com.dota2coach.core.model.Item;
import com.dota2coach.core.model.MatchPlayer;
import com.dota2coach.core.model.MatchSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compara quando o jogador e o adversário direto terminaram os itens principais com quando os heróis deles
 * costumam terminá-los em partidas públicas.
 */
final class MatchItemTimingCalculator {

    /** Partidas mínimas numa faixa de tempo para mostrar a taxa de vitória dela. */
    static final int MIN_BIN_GAMES = 20;
    /** Partidas mínimas no total do item para comparar o tempo. */
    static final int MIN_ITEM_GAMES = 50;

    private MatchItemTimingCalculator() {}

    /**
     * @param directOpponent adversário direto; sem ele, entram os adversários que passaram a fase de rotas na
     *     sua rota
     */
    static List<ItemTimingLine> calculate(MatchSnapshot snapshot, MatchPlayer me, MatchPlayer directOpponent,
            Map<String, Item> itemsByKey) {
        if (!snapshot.match().replayParsed()) {
            return List.of();
        }
        List<MatchPlayer> players = new ArrayList<>();
        players.add(me);
        if (directOpponent != null) {
            players.add(directOpponent);
        } else if (me.physicalLane() != null && me.physicalLane() <= 3) {
            snapshot.match().players().stream()
                    .filter(p -> p.side() != me.side() && Objects.equals(p.physicalLane(), me.physicalLane()))
                    .forEach(players::add);
        }
        List<ItemTimingLine> lines = new ArrayList<>();
        for (MatchPlayer p : players) {
            HeroItemTimings timings = snapshot.itemTimings().get(p.heroId());
            if (timings == null) {
                continue;
            }
            MatchItemsCalculator.firstCoreItems(p, itemsByKey).forEach((key, time) -> {
                List<HeroItemTimings.Bin> bins = timings.byItem().get(key);
                if (bins == null || bins.stream().mapToInt(HeroItemTimings.Bin::games).sum() < MIN_ITEM_GAMES) {
                    return;
                }
                lines.add(line(snapshot.heroName(p.heroId()), p.equals(me), itemsByKey.get(key).name(), time, bins));
            });
        }
        return lines;
    }

    static ItemTimingLine line(String heroName, boolean you, String itemName, int time,
            List<HeroItemTimings.Bin> bins) {
        int total = bins.stream().mapToInt(HeroItemTimings.Bin::games).sum();
        int median = 0;
        int cumulative = 0;
        for (int i = 0; i < bins.size(); i++) {
            cumulative += bins.get(i).games();
            if (cumulative * 2 >= total) {
                median = i;
                break;
            }
        }
        int yours = bins.size();
        for (int i = 0; i < bins.size(); i++) {
            if (time <= bins.get(i).upToSeconds()) {
                yours = i;
                break;
            }
        }
        String comparison = yours < median ? "antes do normal" : yours == median ? "no normal" : "depois do normal";
        Double winRate = null;
        if (yours < bins.size() && bins.get(yours).games() >= MIN_BIN_GAMES) {
            winRate = bins.get(yours).wins() / (double) bins.get(yours).games();
        }
        return new ItemTimingLine(heroName, you, itemName, time, range(bins, median), comparison, winRate);
    }

    static String range(List<HeroItemTimings.Bin> bins, int index) {
        String to = minutes(bins.get(index).upToSeconds());
        return index == 0 ? "até " + to + " min" : minutes(bins.get(index - 1).upToSeconds()) + " a " + to + " min";
    }

    private static String minutes(int seconds) {
        return seconds % 60 == 0 ? String.valueOf(seconds / 60)
                : String.valueOf(Math.round(seconds / 6.0) / 10.0).replace('.', ',');
    }
}
