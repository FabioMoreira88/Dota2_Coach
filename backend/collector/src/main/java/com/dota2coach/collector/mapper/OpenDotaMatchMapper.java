package com.dota2coach.collector.mapper;

import com.dota2coach.collector.dto.opendota.OpenDotaMatchResponse;
import com.dota2coach.core.enums.GameMode;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Side;
import com.dota2coach.core.model.MatchRecord;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Converte partidas da OpenDota em {@link MatchRecord}, traduzindo os códigos da Valve para enums do domínio. */
public final class OpenDotaMatchMapper {

    /** player_slot a partir de 128 é o lado Dire. */
    private static final int DIRE_SLOT_START = 128;
    private static final int LOBBY_RANKED = 7;
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
