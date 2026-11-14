package com.dota2coach.collector.mapper;

import static org.assertj.core.api.Assertions.assertThat;

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

    private static final java.util.Map<String, Integer> HERO_IDS = java.util.Map.of(
            "npc_dota_hero_pudge", 14, "npc_dota_hero_invoker", 74, "npc_dota_hero_antimage", 1,
            "npc_dota_hero_axe", 2);

    private static AnalyzedMatch fullMatch() throws IOException {
        OpenDotaMatchDetailResponse detail = new ObjectMapper().readValue(
                new org.springframework.core.io.ClassPathResource("opendota/match-full.json").getInputStream(),
                OpenDotaMatchDetailResponse.class);
        return OpenDotaMatchMapper.toAnalyzedMatch(detail, HERO_IDS).orElseThrow();
    }

    @Test
    void converteAPartidaInteiraComOsDezJogadores() throws IOException {
        AnalyzedMatch match = fullMatch();

        assertThat(match.replayParsed()).isTrue();
        assertThat(match.ranked()).isTrue();
        assertThat(match.radiantWin()).isFalse();
        assertThat(match.duration()).isEqualTo(Duration.ofMinutes(40));
        assertThat(match.players()).hasSize(4);
        MatchPlayer me = match.player(34839344L).orElseThrow();
        assertThat(me.heroId()).isEqualTo(14);
        assertThat(me.side()).isEqualTo(Side.RADIANT);
        assertThat(me.lane()).isEqualTo(Lane.MID_LANE);
        assertThat(me.physicalLane()).isEqualTo(2);
        assertThat(MatchPlayer.atMinute(me.goldByMinute(), 10)).isEqualTo(3200);
        assertThat(me.observersPlaced()).isEqualTo(1);
        assertThat(me.sentriesPlaced()).isEqualTo(2);
        assertThat(me.laneEfficiencyPct()).isEqualTo(78);
        assertThat(me.benchmarkPercentiles()).containsEntry(BenchmarkMetric.GOLD_PER_MIN, 0.62)
                .doesNotContainKey(BenchmarkMetric.DEATHS_PER_MIN);
        assertThat(me.lastHitsAt10Percentile()).isEqualTo(0.45);
        assertThat(me.killLog()).containsExactly(new MatchPlayer.Kill(700, 74));
        assertThat(match.players().get(1).accountId()).isNull();
        assertThat(me.damageReceivedBySource()).containsEntry("null", 4000L).containsEntry("invoker_sun_strike", 2000L);
        assertThat(match.picks()).containsExactly(
                new AnalyzedMatch.DraftPick(1, 14, Side.RADIANT), new AnalyzedMatch.DraftPick(2, 2, Side.RADIANT),
                new AnalyzedMatch.DraftPick(3, 74, Side.DIRE), new AnalyzedMatch.DraftPick(4, 1, Side.DIRE));
    }

    @Test
    void semDraftUsaOsHeroisDosJogadoresSemOrdem() {
        var detail = new OpenDotaMatchDetailResponse(1L, 0L, 1800, true, 22, 7, null, null, null, List.of(),
                List.of(new OpenDotaMatchDetailResponse.Player(130, null, 74, false, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null)), null, null);

        AnalyzedMatch match = OpenDotaMatchMapper.toAnalyzedMatch(detail, HERO_IDS).orElseThrow();
        assertThat(match.picks()).containsExactly(new AnalyzedMatch.DraftPick(null, 74, Side.DIRE));
        assertThat(match.objectives()).isEmpty();
        assertThat(match.teamfights()).isEmpty();
    }

    @Test
    void converteObjetivosEmOrdemDeTempoPeloTimeQueConquistou() throws IOException {
        assertThat(fullMatch().objectives())
                .as("aegis, courier e construções que não são torre, barracks ou Ancient ficam de fora")
                .containsExactly(
                        new AnalyzedMatch.Objective(95, AnalyzedMatch.ObjectiveKind.FIRST_BLOOD, Side.DIRE, null),
                        new AnalyzedMatch.Objective(854, AnalyzedMatch.ObjectiveKind.BUILDING, Side.RADIANT,
                                "npc_dota_badguys_tower1_mid"),
                        new AnalyzedMatch.Objective(1500, AnalyzedMatch.ObjectiveKind.ROSHAN, Side.DIRE, null),
                        new AnalyzedMatch.Objective(1700, AnalyzedMatch.ObjectiveKind.BUILDING, Side.DIRE,
                                "npc_dota_goodguys_range_rax_bot"));
    }

    @Test
    void ligaCadaJogadorDaLutaAoHeroiPelaOrdemDosSlots() throws IOException {
        List<AnalyzedMatch.Teamfight> fights = fullMatch().teamfights();

        assertThat(fights).as("a luta sem os jogadores todos fica de fora").hasSize(1);
        AnalyzedMatch.Teamfight fight = fights.get(0);
        assertThat(fight.start()).isEqualTo(1185);
        assertThat(fight.end()).isEqualTo(1230);
        assertThat(fight.players()).containsExactly(
                new AnalyzedMatch.FightPlayer(14, Side.RADIANT, 1, -150, 800, 0, 0),
                new AnalyzedMatch.FightPlayer(2, Side.RADIANT, 1, -200, 400, 0, 1),
                new AnalyzedMatch.FightPlayer(74, Side.DIRE, 0, 600, 2000, 0, 0),
                new AnalyzedMatch.FightPlayer(1, Side.DIRE, 1, 250, 900, 0, 0));
    }

    @Test
    void identificaQuemMatouPeloDeathsLogOuPeloKillsLogDoAdversario() throws IOException {
        MatchPlayer me = fullMatch().player(34839344L).orElseThrow();

        assertThat(me.deathLog()).containsExactly(
                new MatchPlayer.Death(300, 12, 74),
                new MatchPlayer.Death(1500, 40, 1));
    }

    @Test
    void partidaSemResultadoNaoViraAnalise() {
        var detail = new OpenDotaMatchDetailResponse(1L, null, null, null, null, null, null, null, null, null, List.of(), null, null);

        assertThat(OpenDotaMatchMapper.toAnalyzedMatch(detail, HERO_IDS)).isEmpty();
    }
}
