package com.dota2coach.analysis.metric;

import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Position;
import com.dota2coach.core.enums.Side;
import com.dota2coach.core.model.AnalyzedMatch;
import com.dota2coach.core.model.MatchPlayer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Estima a posição (1 a 5) dos jogadores pela rota e pelo farm, porque a OpenDota não informa a posição escolhida
 * no matchmaking. Na rota segura, quem mais farmou é o 1 e o outro o 5; na difícil, quem mais farmou é o 3 e o
 * outro o 4; o meio é o 2. Quem sobrar (selva, roaming, rota desconhecida) fica com as posições livres, as de
 * core para quem farmou mais. Nunca é usada para a posição do próprio jogador, que é sempre a informada.
 */
final class PositionEstimator {

    /** Posição de quem o jogador enfrenta diretamente: 1 contra 3, 2 contra 2, 4 contra 5. */
    private static final Map<Integer, Integer> COUNTERPART = Map.of(1, 3, 2, 2, 3, 1, 4, 5, 5, 4);

    private static final int CARRY = 1;
    private static final int HARD_SUPPORT = 5;

    private PositionEstimator() {}

    /**
     * Se o herói foi jogado numa posição que não combina com as funções oficiais dele: um herói de carry (sem a
     * função de suporte) de 5, ou um suporte (sem a função de carry) de 1. A posição 4 fica de fora porque muitos
     * heróis listados só como carry são jogados de 4 (Spirit Breaker, por exemplo).
     */
    static boolean unusual(List<String> heroRoles, int position) {
        boolean carry = heroRoles.contains("Carry");
        boolean support = heroRoles.contains("Support");
        return (position == HARD_SUPPORT && carry && !support) || (position == CARRY && support && !carry);
    }

    /**
     * Posição estimada e o motivo.
     *
     * @param basis de onde a estimativa veio, ex.: "rota segura, menor patrimônio do time"
     */
    record Estimate(int position, String basis) {}

    /** Posição estimada por id do herói. Vazio sem replay analisado, porque aí as rotas são desconhecidas. */
    static Map<Integer, Estimate> estimate(AnalyzedMatch match) {
        Map<Integer, Estimate> estimates = new HashMap<>();
        if (!match.replayParsed()) {
            return estimates;
        }
        for (Side side : Side.values()) {
            List<MatchPlayer> team = match.players().stream().filter(p -> p.side() == side)
                    .sorted(Comparator.comparingInt(MatchPlayer::netWorth).reversed()).toList();
            positions(team).forEach((heroId, position) -> {
                MatchPlayer p = team.stream().filter(t -> t.heroId() == heroId).findFirst().orElseThrow();
                estimates.put(heroId, new Estimate(position,
                        p.lane().label().toLowerCase(Locale.ROOT) + ", " + farmRank(team.indexOf(p),
                                team.size())));
            });
        }
        return estimates;
    }

    private static String farmRank(int index, int size) {
        if (index == 0) {
            return "maior patrimônio do time";
        }
        if (index == size - 1) {
            return "menor patrimônio do time";
        }
        return (index + 1) + "º patrimônio do time";
    }

    private static Map<Integer, Integer> positions(List<MatchPlayer> team) {
        Map<Integer, Integer> positions = new HashMap<>();
        if (!team.stream().allMatch(p -> p.lane() == Lane.UNKNOWN)) {
            TreeSet<Integer> free = new TreeSet<>(List.of(1, 2, 3, 4, 5));
            List<MatchPlayer> left = new ArrayList<>();
            assign(byFarm(team, Lane.MID_LANE), List.of(2), free, positions, left);
            assign(byFarm(team, Lane.SAFE_LANE), List.of(1, 5), free, positions, left);
            assign(byFarm(team, Lane.OFF_LANE), List.of(3, 4), free, positions, left);
            team.stream()
                    .filter(p -> p.lane() != Lane.MID_LANE && p.lane() != Lane.SAFE_LANE && p.lane() != Lane.OFF_LANE)
                    .forEach(left::add);
            left.sort(Comparator.comparingInt(MatchPlayer::netWorth).reversed());
            for (MatchPlayer p : left) {
                if (!free.isEmpty()) {
                    positions.put(p.heroId(), free.pollFirst());
                }
            }
        }
        return positions;
    }

    /**
     * O adversário que o jogador enfrenta diretamente pela posição que ele informou: o 2 adversário para quem
     * joga de 2, o 3 para quem joga de 1, e assim por diante. Vazio sem posição informada ou sem estimativa.
     */
    static Optional<MatchPlayer> directOpponent(AnalyzedMatch match, MatchPlayer me, Position position,
            Map<Integer, Estimate> estimated) {
        if (position == null) {
            return Optional.empty();
        }
        int target = COUNTERPART.get(position.number());
        return match.players().stream()
                .filter(p -> p.side() != me.side())
                .filter(p -> estimated.containsKey(p.heroId()) && estimated.get(p.heroId()).position() == target)
                .findFirst();
    }

    private static List<MatchPlayer> byFarm(List<MatchPlayer> team, Lane lane) {
        return team.stream().filter(p -> p.lane() == lane)
                .sorted(Comparator.comparingInt(MatchPlayer::netWorth).reversed()).toList();
    }

    private static void assign(List<MatchPlayer> players, List<Integer> wanted, TreeSet<Integer> free,
            Map<Integer, Integer> positions, List<MatchPlayer> left) {
        for (int i = 0; i < players.size(); i++) {
            Integer position = i < wanted.size() ? wanted.get(i) : null;
            if (position != null && free.remove(position)) {
                positions.put(players.get(i).heroId(), position);
            } else {
                left.add(players.get(i));
            }
        }
    }
}
