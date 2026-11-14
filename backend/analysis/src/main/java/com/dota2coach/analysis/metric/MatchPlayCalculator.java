package com.dota2coach.analysis.metric;

import com.dota2coach.analysis.metric.MatchAnalysis.DamageFrom;
import com.dota2coach.analysis.metric.MatchAnalysis.PlayDetails;
import com.dota2coach.analysis.metric.MatchAnalysis.Share;
import com.dota2coach.analysis.metric.MatchAnalysis.Use;
import com.dota2coach.core.model.Item;
import com.dota2coach.core.model.MatchPlayer;
import com.dota2coach.core.model.MatchSnapshot;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.UnaryOperator;

/** Dano recebido por herói, uso de itens e habilidades, fontes de ouro e XP, visão e mapa do jogador. */
final class MatchPlayCalculator {

    static final int MAX_ITEM_USES = 8;
    static final int MAX_ABILITY_USES = 6;

    /** Códigos de ouro da Valve (EDOTA_ModifyGold_Reason). */
    static final int GOLD_DEATH = 1;
    static final int GOLD_BUYBACK = 2;
    static final int GOLD_SELL = 6;
    private static final Map<Integer, String> GOLD_SOURCES = Map.of(
            11, "Construções", 12, "Heróis", 13, "Creeps de rota", 14, "Neutros", 15, "Roshan", 17,
            "Runas de bounty");
    /** Códigos de XP da Valve (EDOTA_ModifyXP_Reason). */
    private static final Map<Integer, String> XP_SOURCES = Map.of(1, "Heróis", 2, "Creeps e neutros", 3, "Roshan");
    /** Tipos de runa da Valve. */
    private static final Map<Integer, String> RUNES = Map.of(0, "Dano dobrado", 1, "Velocidade", 2, "Ilusão", 3,
            "Invisibilidade", 4, "Regeneração", 5, "Bounty", 6, "Arcana", 7, "Água", 8, "Sabedoria", 9, "Escudo");
    private static final String OTHERS = "Outros";

    private MatchPlayCalculator() {}

    static PlayDetails calculate(MatchSnapshot snapshot, MatchPlayer me, Map<String, Item> itemsByKey) {
        if (!snapshot.match().replayParsed()) {
            return PlayDetails.EMPTY;
        }
        MatchPlayer.ReplayStats s = me.stats();
        long fromHeroes = s.damageTakenFromHeroes().values().stream().mapToLong(Long::longValue).sum();
        long totalTaken = fromHeroes + s.damageTakenFromOthers();
        List<DamageFrom> damage = s.damageTakenFromHeroes().entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .map(e -> new DamageFrom(snapshot.heroName(e.getKey()), e.getValue(),
                        totalTaken == 0 ? 0 : e.getValue() / (double) totalTaken))
                .toList();
        Map<Integer, Long> gold = s.goldByReason();
        return new PlayDetails(
                damage,
                totalTaken == 0 ? 0 : s.damageTakenFromOthers() / (double) totalTaken,
                Math.max(0, -gold.getOrDefault(GOLD_DEATH, 0L)),
                Math.max(0, -gold.getOrDefault(GOLD_BUYBACK, 0L)),
                shares(gold, GOLD_SOURCES, code -> code != GOLD_DEATH && code != GOLD_BUYBACK && code != GOLD_SELL),
                shares(s.xpByReason(), XP_SOURCES, code -> true),
                uses(s.itemUses(), key -> itemsByKey.containsKey(key) ? itemsByKey.get(key).name() : key,
                        MAX_ITEM_USES),
                uses(s.abilityUses(), key -> abilityName(snapshot, key), MAX_ABILITY_USES),
                s.stunSeconds(),
                s.observerKills(),
                s.sentryKills(),
                s.campsStacked(),
                s.runes().entrySet().stream()
                        .filter(e -> e.getValue() > 0)
                        .map(e -> new Use(RUNES.getOrDefault(e.getKey(), OTHERS), e.getValue()))
                        .sorted(Comparator.comparingLong(Use::count).reversed().thenComparing(Use::name))
                        .toList());
    }

    /** Partes positivas por rótulo, juntando os códigos sem rótulo em "Outros", da maior para a menor. */
    static List<Share> shares(Map<Integer, Long> byCode, Map<Integer, String> labels,
            IntPredicate include) {
        Map<String, Long> totals = new LinkedHashMap<>();
        byCode.forEach((code, value) -> {
            if (value > 0 && include.test(code)) {
                totals.merge(labels.getOrDefault(code, OTHERS), value, Long::sum);
            }
        });
        long total = totals.values().stream().mapToLong(Long::longValue).sum();
        return totals.entrySet().stream()
                .map(e -> new Share(e.getKey(), e.getValue(), total == 0 ? 0 : e.getValue() / (double) total))
                .sorted(Comparator.comparingLong(Share::value).reversed().thenComparing(Share::label))
                .toList();
    }

    /** Ações do mapa que o replay registra como habilidade, sem nome de exibição nas constantes do OpenDota. */
    static final Map<String, String> MAP_ACTIONS = Map.of(
            "ability_capture", "Captura de outpost",
            "abyssal_underlord_portal_warp", "Portal do Underlord",
            "twin_gate_portal_warp", "Twin Gate",
            "ability_lamp_use", "Lotus Pool",
            "ability_pluck_famango", "Famango");

    /** Nome legível: o das constantes, o de uma ação do mapa ou, sem nenhum dos dois, a chave sem os sublinhados. */
    static String abilityName(MatchSnapshot snapshot, String key) {
        String name = snapshot.abilityNames().get(key);
        if (name != null && !name.isBlank()) {
            return name;
        }
        return MAP_ACTIONS.getOrDefault(key, key.replace('_', ' '));
    }

    private static List<Use> uses(Map<String, Long> byKey, UnaryOperator<String> name,
            int limit) {
        return byKey.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(e -> new Use(name.apply(e.getKey()), e.getValue()))
                .toList();
    }
}
