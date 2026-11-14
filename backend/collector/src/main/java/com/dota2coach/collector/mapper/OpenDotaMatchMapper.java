package com.dota2coach.collector.mapper;

import com.dota2coach.collector.dto.opendota.OpenDotaMatchDetailResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaMatchResponse;
import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.GameMode;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Side;
import com.dota2coach.core.model.AnalyzedMatch;
import com.dota2coach.core.model.MatchDetail;
import com.dota2coach.core.model.MatchPlayer;
import com.dota2coach.core.model.MatchRecord;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Converte partidas da OpenDota em {@link MatchRecord}, traduzindo os códigos da Valve para enums do domínio. */
public final class OpenDotaMatchMapper {

    /** player_slot a partir de 128 é o lado Dire. */
    private static final int DIRE_SLOT_START = 128;
    private static final int LOBBY_RANKED = 7;
    /** Times nos eventos de objetivo da OpenDota. */
    private static final int TEAM_RADIANT = 2;
    private static final int TEAM_DIRE = 3;
    /** leaver_status 0 = ficou, 1 = desconectou e voltou; a partir de 2 conta como abandono. */
    private static final int LEAVER_ABANDON_START = 2;

    private OpenDotaMatchMapper() {}

    /**
     * Converte a partida, ou devolve vazio se ela não tem resultado (partida antiga ou anulada).
     */
    public static Optional<MatchRecord> toMatchRecord(OpenDotaMatchResponse m) {
        if (m.radiantWin() == null || m.playerSlot() == null || m.duration() == null) {
            return Optional.empty();
        }
        Side side = m.playerSlot() >= DIRE_SLOT_START ? Side.DIRE : Side.RADIANT;
        boolean won = (side == Side.RADIANT) == m.radiantWin();
        List<Integer> items = Stream.of(m.item0(), m.item1(), m.item2(), m.item3(), m.item4(), m.item5())
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .toList();
        return Optional.of(new MatchRecord(
                m.matchId(),
                Instant.ofEpochSecond(m.startTime()),
                Duration.ofSeconds(m.duration()),
                m.heroId(),
                side,
                won,
                gameMode(m.gameMode()),
                Objects.equals(m.lobbyType(), LOBBY_RANKED),
                orZero(m.partySize()),
                orZero(m.leaverStatus()) >= LEAVER_ABANDON_START,
                orZero(m.kills()),
                orZero(m.deaths()),
                orZero(m.assists()),
                orZero(m.goldPerMin()),
                orZero(m.xpPerMin()),
                orZero(m.lastHits()),
                orZero(m.denies()),
                orZero(m.heroDamage()),
                orZero(m.towerDamage()),
                orZero(m.heroHealing()),
                lane(m.laneRole(), m.isRoaming()),
                items,
                orZero(m.itemNeutral()),
                m.version() != null));
    }

    public static List<MatchRecord> toMatchRecords(List<OpenDotaMatchResponse> matches) {
        return matches.stream().map(OpenDotaMatchMapper::toMatchRecord).flatMap(Optional::stream).toList();
    }

    /**
     * Extrai os detalhes do jogador (identificado pelo herói, único na partida) de uma partida analisada.
     *
     * @return vazio se o herói não está na partida ou se o replay não foi analisado (sem last hits por minuto)
     */
    public static Optional<MatchDetail> toMatchDetail(OpenDotaMatchDetailResponse detail, int heroId) {
        if (detail == null || detail.players() == null) {
            return Optional.empty();
        }
        return detail.players().stream()
                .filter(p -> p != null && Objects.equals(p.heroId(), heroId))
                .filter(p -> p.lastHitsByMinute() != null && !p.lastHitsByMinute().isEmpty())
                .findFirst()
                .map(p -> new MatchDetail(
                        detail.matchId(),
                        heroId,
                        atMinute(p.lastHitsByMinute(), 10),
                        atMinute(p.deniesByMinute(), 10),
                        purchases(p.purchaseLog()),
                        p.deathsLog() == null ? List.of() : p.deathsLog().stream()
                                .filter(d -> d != null && d.time() != null)
                                .map(d -> new MatchDetail.Death(d.time(), orZero(d.timeDead())))
                                .toList()));
    }

    /**
     * Converte a partida inteira para a análise de partida escolhida.
     *
     * @param heroIdByName id do herói pelo nome interno (ex.: "npc_dota_hero_lina"), para saber quem matou quem
     * @return vazio se a partida ainda não tem resultado
     */
    public static Optional<AnalyzedMatch> toAnalyzedMatch(
            OpenDotaMatchDetailResponse detail, Map<String, Integer> heroIdByName) {
        if (detail == null || detail.players() == null || detail.radiantWin() == null || detail.duration() == null) {
            return Optional.empty();
        }
        List<OpenDotaMatchDetailResponse.Player> players = detail.players().stream()
                .filter(p -> p != null && p.heroId() != null && p.playerSlot() != null)
                .toList();
        return Optional.of(new AnalyzedMatch(
                detail.matchId(),
                Instant.ofEpochSecond(detail.startTime() == null ? 0 : detail.startTime()),
                Duration.ofSeconds(detail.duration()),
                detail.radiantWin(),
                gameMode(detail.gameMode()),
                Objects.equals(detail.lobbyType(), LOBBY_RANKED),
                detail.version() != null,
                orEmpty(detail.radiantGoldAdvantage()),
                picks(detail, players),
                players.stream().map(p -> toMatchPlayer(p, players, heroIdByName)).toList(),
                objectives(detail.objectives()),
                teamfights(detail.teamfights(), players)));
    }

    /** Torres, barracks e trono destruídos, Roshans e first blood, em ordem de tempo. */
    static List<AnalyzedMatch.Objective> objectives(List<OpenDotaMatchDetailResponse.Objective> raw) {
        if (raw == null) {
            return List.of();
        }
        List<AnalyzedMatch.Objective> objectives = new ArrayList<>();
        for (var o : raw) {
            if (o == null || o.time() == null || o.type() == null) {
                continue;
            }
            switch (o.type()) {
                case "building_kill" -> {
                    String key = o.key() instanceof String k ? k : null;
                    if (key != null && (key.contains("_tower") || key.contains("_rax_") || key.endsWith("_fort"))) {
                        Side owner = key.contains("goodguys") ? Side.RADIANT
                                : key.contains("badguys") ? Side.DIRE : null;
                        if (owner != null) {
                            objectives.add(new AnalyzedMatch.Objective(o.time(), AnalyzedMatch.ObjectiveKind.BUILDING,
                                    owner == Side.RADIANT ? Side.DIRE : Side.RADIANT, key));
                        }
                    }
                }
                case "CHAT_MESSAGE_ROSHAN_KILL" -> {
                    if (o.team() != null && (o.team() == TEAM_RADIANT || o.team() == TEAM_DIRE)) {
                        objectives.add(new AnalyzedMatch.Objective(o.time(), AnalyzedMatch.ObjectiveKind.ROSHAN,
                                o.team() == TEAM_RADIANT ? Side.RADIANT : Side.DIRE, null));
                    }
                }
                case "CHAT_MESSAGE_FIRSTBLOOD" -> {
                    if (o.playerSlot() != null) {
                        objectives.add(new AnalyzedMatch.Objective(o.time(),
                                AnalyzedMatch.ObjectiveKind.FIRST_BLOOD,
                                o.playerSlot() >= DIRE_SLOT_START ? Side.DIRE : Side.RADIANT, null));
                    }
                }
                default -> { }
            }
        }
        objectives.sort(Comparator.comparingInt(AnalyzedMatch.Objective::time));
        return objectives;
    }

    /**
     * Lutas, ligando cada posição da lista de jogadores da luta ao herói pela ordem dos slots (Radiant primeiro).
     * Lutas sem os dez jogadores ficam de fora, porque não dá para saber quem é quem.
     */
    static List<AnalyzedMatch.Teamfight> teamfights(List<OpenDotaMatchDetailResponse.Teamfight> raw,
            List<OpenDotaMatchDetailResponse.Player> players) {
        if (raw == null) {
            return List.of();
        }
        List<OpenDotaMatchDetailResponse.Player> bySlot = players.stream()
                .sorted(Comparator.comparingInt(OpenDotaMatchDetailResponse.Player::playerSlot))
                .toList();
        List<AnalyzedMatch.Teamfight> fights = new ArrayList<>();
        for (var tf : raw) {
            if (tf == null || tf.start() == null || tf.end() == null || tf.players() == null
                    || tf.players().size() != bySlot.size()) {
                continue;
            }
            List<AnalyzedMatch.FightPlayer> fighters = new ArrayList<>();
            for (int i = 0; i < bySlot.size(); i++) {
                var p = bySlot.get(i);
                var f = tf.players().get(i);
                if (f == null) {
                    continue;
                }
                fighters.add(new AnalyzedMatch.FightPlayer(p.heroId(),
                        p.playerSlot() >= DIRE_SLOT_START ? Side.DIRE : Side.RADIANT,
                        orZero(f.deaths()), orZero(f.goldDelta()), f.damage() == null ? 0 : f.damage(),
                        f.healing() == null ? 0 : f.healing(), orZero(f.buybacks())));
            }
            fights.add(new AnalyzedMatch.Teamfight(tf.start(), tf.end(), fighters));
        }
        fights.sort(Comparator.comparingInt(AnalyzedMatch.Teamfight::start));
        return fights;
    }

    /**
     * Picks na ordem do draft: pelo picks_bans da Valve, ou pelo draft lido do replay. Sem nenhum dos dois, os
     * heróis dos jogadores, sem ordem.
     */
    static List<AnalyzedMatch.DraftPick> picks(OpenDotaMatchDetailResponse detail,
            List<OpenDotaMatchDetailResponse.Player> players) {
        List<OpenDotaMatchDetailResponse.PickBan> fromValve = detail.picksBans() == null ? List.of()
                : detail.picksBans().stream()
                        .filter(pb -> pb != null && Boolean.TRUE.equals(pb.pick()) && pb.heroId() != null
                                && pb.team() != null && pb.order() != null)
                        .sorted(Comparator.comparingInt(OpenDotaMatchDetailResponse.PickBan::order))
                        .toList();
        if (!fromValve.isEmpty()) {
            List<AnalyzedMatch.DraftPick> picks = new ArrayList<>();
            for (var pb : fromValve) {
                picks.add(new AnalyzedMatch.DraftPick(picks.size() + 1, pb.heroId(),
                        pb.team() == 0 ? Side.RADIANT : Side.DIRE));
            }
            return picks;
        }
        List<OpenDotaMatchDetailResponse.DraftTiming> fromReplay = detail.draftTimings() == null ? List.of()
                : detail.draftTimings().stream()
                        .filter(d -> d != null && Boolean.TRUE.equals(d.pick()) && d.heroId() != null
                                && d.playerSlot() != null && d.order() != null)
                        .sorted(Comparator.comparingInt(OpenDotaMatchDetailResponse.DraftTiming::order))
                        .toList();
        if (!fromReplay.isEmpty()) {
            List<AnalyzedMatch.DraftPick> picks = new ArrayList<>();
            for (var d : fromReplay) {
                picks.add(new AnalyzedMatch.DraftPick(picks.size() + 1, d.heroId(),
                        d.playerSlot() >= DIRE_SLOT_START ? Side.DIRE : Side.RADIANT));
            }
            return picks;
        }
        return players.stream()
                .map(p -> new AnalyzedMatch.DraftPick(null, p.heroId(),
                        p.playerSlot() >= DIRE_SLOT_START ? Side.DIRE : Side.RADIANT))
                .toList();
    }

    private static MatchPlayer toMatchPlayer(OpenDotaMatchDetailResponse.Player p,
            List<OpenDotaMatchDetailResponse.Player> all, Map<String, Integer> heroIdByName) {
        Side side = p.playerSlot() >= DIRE_SLOT_START ? Side.DIRE : Side.RADIANT;
        Map<BenchmarkMetric, Double> percentiles = new EnumMap<>(BenchmarkMetric.class);
        Double lastHitsAt10Percentile = null;
        if (p.benchmarks() != null) {
            for (BenchmarkMetric metric : BenchmarkMetric.values()) {
                var b = p.benchmarks().get(metric.openDotaKey());
                if (b != null && b.pct() != null) {
                    percentiles.put(metric, b.pct());
                }
            }
            var lhTen = p.benchmarks().get("lhten");
            lastHitsAt10Percentile = lhTen == null ? null : lhTen.pct();
        }
        boolean parsed = p.lastHitsByMinute() != null && !p.lastHitsByMinute().isEmpty();
        return new MatchPlayer(
                p.playerSlot(),
                p.accountId(),
                p.heroId(),
                side,
                parsed ? lane(p.laneRole(), p.roaming()) : Lane.UNKNOWN,
                p.lane(),
                orZero(p.kills()),
                orZero(p.deaths()),
                orZero(p.assists()),
                orZero(p.lastHits()),
                orZero(p.denies()),
                orZero(p.goldPerMin()),
                orZero(p.xpPerMin()),
                orZero(p.heroDamage()),
                orZero(p.towerDamage()),
                orZero(p.heroHealing()),
                orZero(p.netWorth()),
                orZero(p.level()),
                orEmpty(p.lastHitsByMinute()),
                orEmpty(p.deniesByMinute()),
                orEmpty(p.goldByMinute()),
                orEmpty(p.xpByMinute()),
                purchases(p.purchaseLog()),
                deaths(p, all, heroIdByName),
                p.killsLog() == null ? List.of() : p.killsLog().stream()
                        .filter(k -> k != null && k.time() != null)
                        .map(k -> new MatchPlayer.Kill(k.time(), heroIdByName.get(k.key())))
                        .toList(),
                p.observerLog() == null ? 0 : p.observerLog().size(),
                p.sentryLog() == null ? 0 : p.sentryLog().size(),
                p.laneEfficiencyPct(),
                percentiles,
                lastHitsAt10Percentile,
                orEmpty(p.damageInflictor()),
                orEmpty(p.damageInflictorReceived()),
                parsed ? replayStats(p, heroIdByName) : MatchPlayer.ReplayStats.EMPTY);
    }

    private static MatchPlayer.ReplayStats replayStats(OpenDotaMatchDetailResponse.Player p,
            Map<String, Integer> heroIdByName) {
        Map<Integer, Long> fromHeroes = new HashMap<>();
        long fromOthers = 0;
        for (var e : orEmpty(p.damageTaken()).entrySet()) {
            Integer heroId = heroIdByName.get(e.getKey());
            if (heroId != null) {
                fromHeroes.merge(heroId, e.getValue(), Long::sum);
            } else if (e.getKey() == null || !e.getKey().startsWith("npc_dota_hero_")) {
                fromOthers += e.getValue();
            }
        }
        return new MatchPlayer.ReplayStats(
                fromHeroes,
                fromOthers,
                orEmpty(p.abilityUses()),
                orEmpty(p.itemUses()),
                p.stuns() == null ? 0 : p.stuns(),
                byCode(p.goldReasons()),
                byCode(p.xpReasons()),
                byCode(p.runes()),
                orZero(p.campsStacked()),
                orZero(p.observerKills()),
                orZero(p.sentryKills()));
    }

    /** Mapa com chave numérica em texto (código da Valve); chaves que não são número ficam de fora. */
    private static Map<Integer, Long> byCode(Map<String, Long> raw) {
        Map<Integer, Long> codes = new HashMap<>();
        orEmpty(raw).forEach((k, v) -> {
            try {
                codes.merge(Integer.parseInt(k), v, Long::sum);
            } catch (NumberFormatException ignored) {
                // código desconhecido
            }
        });
        return codes;
    }

    /**
     * Mortes com quem matou. Quando o deaths_log não diz o assassino, procura o abate no kills_log dos
     * adversários (mesmo segundo, ou um de diferença).
     */
    private static List<MatchPlayer.Death> deaths(OpenDotaMatchDetailResponse.Player p,
            List<OpenDotaMatchDetailResponse.Player> all, Map<String, Integer> heroIdByName) {
        if (p.deathsLog() == null) {
            return List.of();
        }
        String ownName = heroIdByName.entrySet().stream()
                .filter(e -> e.getValue().equals(p.heroId()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        return p.deathsLog().stream()
                .filter(d -> d != null && d.time() != null)
                .map(d -> {
                    Integer killer = d.key() == null ? null : heroIdByName.get(d.key());
                    if (killer == null && ownName != null) {
                        killer = killerFromKillsLog(d.time(), ownName, p.heroId(), all);
                    }
                    return new MatchPlayer.Death(d.time(), orZero(d.timeDead()), killer);
                })
                .toList();
    }

    private static Integer killerFromKillsLog(int time, String victimName, int victimHeroId,
            List<OpenDotaMatchDetailResponse.Player> all) {
        return all.stream()
                .filter(other -> !Objects.equals(other.heroId(), victimHeroId) && other.killsLog() != null)
                .filter(other -> other.killsLog().stream().anyMatch(k -> k != null && k.time() != null
                        && victimName.equals(k.key()) && Math.abs(k.time() - time) <= 1))
                .map(OpenDotaMatchDetailResponse.Player::heroId)
                .findFirst()
                .orElse(null);
    }

    private static List<MatchDetail.Purchase> purchases(List<OpenDotaMatchDetailResponse.Purchase> log) {
        return log == null ? List.of() : log.stream()
                .filter(b -> b != null && b.time() != null && b.key() != null)
                .map(b -> new MatchDetail.Purchase(b.key(), b.time()))
                .toList();
    }

    private static Map<String, Long> orEmpty(Map<String, Long> map) {
        if (map == null) {
            return Map.of();
        }
        Map<String, Long> clean = new HashMap<>();
        map.forEach((k, v) -> {
            if (v != null) {
                clean.put(k == null ? MatchPlayer.BASIC_ATTACK : k, v);
            }
        });
        return clean;
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list.stream().filter(Objects::nonNull).toList();
    }

    private static Integer atMinute(List<Integer> byMinute, int minute) {
        return byMinute == null || byMinute.size() <= minute ? null : byMinute.get(minute);
    }

    /** Códigos de game_mode da Valve. */
    static GameMode gameMode(Integer code) {
        if (code == null) {
            return GameMode.OTHER;
        }
        return switch (code) {
            case 1, 22 -> GameMode.ALL_PICK;
            case 2 -> GameMode.CAPTAINS_MODE;
            case 3 -> GameMode.RANDOM_DRAFT;
            case 4 -> GameMode.SINGLE_DRAFT;
            case 5 -> GameMode.ALL_RANDOM;
            case 16 -> GameMode.CAPTAINS_DRAFT;
            case 18 -> GameMode.ABILITY_DRAFT;
            case 23 -> GameMode.TURBO;
            default -> GameMode.OTHER;
        };
    }

    /** Códigos de lane_role da OpenDota: 1 segura, 2 meio, 3 difícil, 4 selva. */
    static Lane lane(Integer laneRole, Boolean roaming) {
        if (Boolean.TRUE.equals(roaming)) {
            return Lane.ROAMING;
        }
        if (laneRole == null) {
            return Lane.UNKNOWN;
        }
        return switch (laneRole) {
            case 1 -> Lane.SAFE_LANE;
            case 2 -> Lane.MID_LANE;
            case 3 -> Lane.OFF_LANE;
            case 4 -> Lane.JUNGLE;
            default -> Lane.UNKNOWN;
        };
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
