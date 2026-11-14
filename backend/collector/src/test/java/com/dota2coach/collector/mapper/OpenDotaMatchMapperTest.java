package com.dota2coach.collector.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.dota2coach.collector.dto.opendota.OpenDotaMatchDetailResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaMatchResponse;
import com.dota2coach.core.enums.GameMode;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Side;
import com.dota2coach.core.model.MatchDetail;
import com.dota2coach.core.model.MatchRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenDotaMatchMapperTest {

    private List<MatchRecord> matches;

    @BeforeEach
    void setUp() throws IOException {
        try (InputStream json = getClass().getResourceAsStream("/opendota/matches.json")) {
            List<OpenDotaMatchResponse> raw = new ObjectMapper().readValue(json, new TypeReference<>() {});
            matches = OpenDotaMatchMapper.toMatchRecords(raw);
        }
    }

    @Test
    void ignoraPartidaSemResultado() {
        assertThat(matches).extracting(MatchRecord::matchId)
                .containsExactly(9083198111L, 9082528890L, 9080000000L);
    }

    @Test
    void calculaLadoEResultadoPeloPlayerSlot() {
        MatchRecord direDerrota = matches.get(0);
        assertThat(direDerrota.side()).isEqualTo(Side.DIRE);
        assertThat(direDerrota.won()).isFalse();

        MatchRecord radiantDerrota = matches.get(1);
        assertThat(radiantDerrota.side()).isEqualTo(Side.RADIANT);
        assertThat(radiantDerrota.won()).isFalse();

        assertThat(matches.get(2).won()).isTrue();
    }

    @Test
    void converteCamposDaPartidaAnalisada() {
        MatchRecord m = matches.get(0);
        assertThat(m.startTime()).isEqualTo(Instant.ofEpochSecond(1794700000L));
        assertThat(m.duration()).isEqualTo(Duration.ofSeconds(3370));
        assertThat(m.gameMode()).isEqualTo(GameMode.ALL_PICK);
        assertThat(m.ranked()).isTrue();
        assertThat(m.parsed()).isTrue();
        assertThat(m.lane()).isEqualTo(Lane.OFF_LANE);
        assertThat(m.items()).containsExactly(1, 116, 63);
        assertThat(m.neutralItem()).isEqualTo(300);
        assertThat(m.abandoned()).as("desconectou e voltou não é abandono").isFalse();
        assertThat(m.kda()).isEqualTo(5.0);
    }

    @Test
    void partidaSemReplayAnalisadoFicaComRotaDesconhecidaESemItens() {
        MatchRecord m = matches.get(1);
        assertThat(m.parsed()).isFalse();
        assertThat(m.lane()).isEqualTo(Lane.UNKNOWN);
        assertThat(m.items()).isEmpty();
        assertThat(m.partySize()).isEqualTo(2);
    }

    @Test
    void marcaAbandonoEPartidaNaoRanqueada() {
        MatchRecord m = matches.get(2);
        assertThat(m.abandoned()).isTrue();
        assertThat(m.ranked()).isFalse();
        assertThat(m.lane()).isEqualTo(Lane.SAFE_LANE);
        assertThat(m.kda()).as("zero mortes conta como uma").isEqualTo(13.0);
    }

    @Test
    void roamingTemPrioridadeSobreARota() {
        assertThat(OpenDotaMatchMapper.lane(1, true)).isEqualTo(Lane.ROAMING);
        assertThat(OpenDotaMatchMapper.lane(4, false)).isEqualTo(Lane.JUNGLE);
        assertThat(OpenDotaMatchMapper.lane(9, null)).isEqualTo(Lane.UNKNOWN);
    }

    @Test
    void traduzCodigosDeModoDeJogo() {
        assertThat(OpenDotaMatchMapper.gameMode(23)).isEqualTo(GameMode.TURBO);
        assertThat(OpenDotaMatchMapper.gameMode(2)).isEqualTo(GameMode.CAPTAINS_MODE);
        assertThat(OpenDotaMatchMapper.gameMode(null)).isEqualTo(GameMode.OTHER);
        assertThat(OpenDotaMatchMapper.gameMode(19)).isEqualTo(GameMode.OTHER);
    }

    @Test
    void extraiDetalhesDoJogadorPeloHeroi() throws IOException {
        OpenDotaMatchDetailResponse detail = new ObjectMapper().readValue(
                new org.springframework.core.io.ClassPathResource("opendota/match-detail.json").getInputStream(),
                OpenDotaMatchDetailResponse.class);

        MatchDetail ogre = OpenDotaMatchMapper.toMatchDetail(detail, 84).orElseThrow();

        assertThat(ogre.lastHitsAt10()).isEqualTo(12);
        assertThat(ogre.deniesAt10()).isEqualTo(3);
        assertThat(ogre.purchases()).extracting(MatchDetail.Purchase::itemKey)
                .containsExactly("tango", "arcane_boots", "blink");
        assertThat(ogre.deaths()).containsExactly(new MatchDetail.Death(300, 12), new MatchDetail.Death(1500, 40));
        assertThat(OpenDotaMatchMapper.toMatchDetail(detail, 35)).as("sem lh_t: replay não analisado").isEmpty();
        assertThat(OpenDotaMatchMapper.toMatchDetail(detail, 1)).as("herói fora da partida").isEmpty();
    }
}
