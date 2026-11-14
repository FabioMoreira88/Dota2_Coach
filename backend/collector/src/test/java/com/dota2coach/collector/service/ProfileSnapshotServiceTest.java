package com.dota2coach.collector.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dota2coach.collector.client.opendota.OpenDotaClient;
import com.dota2coach.collector.config.OpenDotaProperties;
import com.dota2coach.collector.support.RequestRateLimiter;
import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.GameMode;
import com.dota2coach.core.enums.GamePhase;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.RankBracket;
import com.dota2coach.core.enums.Side;
import com.dota2coach.core.model.MatchRecord;
import com.dota2coach.core.model.PlayerSummary;
import com.dota2coach.core.model.ProfileSnapshot;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ProfileSnapshotServiceTest {

    private static final String BASE = "https://api.opendota.test/api";
    private static final long ACCOUNT = 34839344L;
    private static final Instant NOW = Instant.ofEpochSecond(1_794_700_000L).plus(Duration.ofDays(1));

    private MockRestServiceServer server;
    private ProfileSnapshotService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        OpenDotaProperties props = new OpenDotaProperties(BASE, "", 6000, 1, null, null, null, null);
        OpenDotaClient client = new OpenDotaClient(
                builder.build(), props, new RequestRateLimiter(6000, () -> 0L, d -> { }), d -> { });
        PlayerDataService players = new PlayerDataService(client, Caffeine.newBuilder().<Long, PlayerSummary>build());
        service = new ProfileSnapshotService(client, players, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void respond(String path, String fixture) {
        server.expect(requestTo(path.contains("?") ? Matchers.startsWith(BASE + path.substring(0, path.indexOf('?')))
                        : Matchers.equalTo(BASE + path)))
                .andRespond(withSuccess(new ClassPathResource("opendota/" + fixture), MediaType.APPLICATION_JSON));
    }

    @Test
    void coletaPerfilPartidasReferenciasEItensDosHeroisRecentes() {
        respond("/players/" + ACCOUNT, "player.json");
        respond("/players/" + ACCOUNT + "/wl", "wl.json");
        respond("/players/" + ACCOUNT + "/heroes", "heroes.json");
        respond("/players/" + ACCOUNT + "/matches?", "matches.json");
        respond("/constants/heroes", "constants-heroes.json");
        respond("/constants/items", "constants-items.json");
        respond("/heroStats", "hero-stats.json");
        respond("/heroes/35/itemPopularity", "item-popularity.json");
        respond("/heroes/84/itemPopularity", "item-popularity.json");
        respond("/benchmarks?hero_id=35", "benchmarks.json");
        respond("/benchmarks?hero_id=84", "benchmarks.json");
        respond("/matches/9083198111", "match-detail.json");
        respond("/matches/9080000000", "match-detail.json");

        ProfileSnapshot snapshot = service.collect(ACCOUNT);

        assertThat(snapshot.player().personaName()).isEqualTo("Jogador Teste");
        assertThat(snapshot.matches()).hasSize(3);
        assertThat(snapshot.heroName(84)).isEqualTo("Ogre Magi");
        assertThat(snapshot.heroName(9999)).isEqualTo("Herói 9999");
        assertThat(snapshot.itemName(116)).isEqualTo("Black King Bar");
        assertThat(snapshot.items()).doesNotContainKey(0);
        assertThat(snapshot.heroMeta().get(35).in(RankBracket.LEGEND).picks()).isEqualTo(5000);
        assertThat(snapshot.itemPopularity()).containsOnlyKeys(35, 84);
        assertThat(snapshot.itemPopularity().get(35).top(GamePhase.START, 1)).containsExactly(44);
        assertThat(snapshot.benchmarks()).containsOnlyKeys(35, 84);
        assertThat(snapshot.benchmarks().get(35).byMetric())
                .as("métrica vazia e sem valor ficam de fora; kills não é usada")
                .containsOnlyKeys(BenchmarkMetric.GOLD_PER_MIN, BenchmarkMetric.XP_PER_MIN,
                        BenchmarkMetric.LAST_HITS_PER_MIN, BenchmarkMetric.DEATHS_PER_MIN);
        assertThat(snapshot.benchmarks().get(35).byMetric().get(BenchmarkMetric.DEATHS_PER_MIN)).hasSize(1);
        assertThat(snapshot.matchDetails()).as("na segunda partida o Sniper não tem replay analisado")
                .containsOnlyKeys(9083198111L);
        assertThat(snapshot.matchDetails().get(9083198111L).lastHitsAt10()).isEqualTo(12);
        assertThat(snapshot.collectedAt()).isEqualTo(NOW);
        server.verify();
    }

    @Test
    void heroisRecentesOrdenadosPorNumeroDePartidas() {
        List<MatchRecord> matches = List.of(
                match(1, 84, NOW, false), match(2, 35, NOW, false), match(3, 35, NOW, false), match(4, 14, NOW, false));

        assertThat(ProfileSnapshotService.recentTopHeroes(matches)).containsExactly(35, 84, 14);
    }

    @Test
    void pedeAnaliseSoDePartidasNaoAnalisadasDentroDoPrazoDoReplay() {
        List<MatchRecord> matches = List.of(
                match(1, 35, NOW.minus(Duration.ofHours(1)), true),
                match(2, 35, NOW.minus(Duration.ofDays(2)), false),
                match(3, 35, NOW.minus(Duration.ofDays(3)), false),
                match(4, 35, NOW.minus(Duration.ofDays(20)), false));
        expectParse(2);
        expectParse(3);
        server.expect(never(), requestTo(BASE + "/request/4"));

        assertThat(service.requestReplayParsing(matches, 10)).containsExactly(2L, 3L);
        server.verify();
    }

    @Test
    void respeitaOLimiteEContinuaQuandoUmPedidoFalha() {
        List<MatchRecord> matches = List.of(
                match(1, 35, NOW, false), match(2, 35, NOW, false), match(3, 35, NOW, false));
        server.expect(requestToUriTemplate(BASE + "/request/{id}", 1)).andRespond(withServerError());
        expectParse(2);
        expectParse(3);

        assertThat(service.requestReplayParsing(matches, 2)).containsExactly(2L, 3L);
        server.verify();
    }

    private void expectParse(long matchId) {
        server.expect(requestToUriTemplate(BASE + "/request/{id}", matchId))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"job\":{\"jobId\":1}}", MediaType.APPLICATION_JSON));
    }

    private static MatchRecord match(long id, int heroId, Instant start, boolean parsed) {
        return new MatchRecord(id, start, Duration.ofMinutes(40), heroId, Side.RADIANT, true, GameMode.ALL_PICK,
                true, 1, false, 5, 5, 5, 500, 600, 200, 5, 20000, 2000, 0, Lane.UNKNOWN, List.of(), 0, parsed);
    }
}
