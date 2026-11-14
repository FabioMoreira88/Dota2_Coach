package com.dota2coach.collector.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dota2coach.collector.client.opendota.OpenDotaClient;
import com.dota2coach.collector.config.OpenDotaProperties;
import com.dota2coach.collector.support.RequestRateLimiter;
import com.dota2coach.core.model.MatchPlayer;
import com.dota2coach.core.model.MatchSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MatchDataServiceTest {

    private static final String BASE = "https://api.opendota.test/api";
    private static final long MATCH = 9083198111L;

    private MockRestServiceServer server;
    private MatchDataService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        OpenDotaProperties props = new OpenDotaProperties(BASE, "", 6000, 1, null, null, null, null);
        OpenDotaClient client = new OpenDotaClient(
                builder.build(), props, new RequestRateLimiter(6000, () -> 0L, d -> { }), d -> { });
        service = new MatchDataService(client);
    }

    private void expect(String path, String fixture) {
        server.expect(requestTo(BASE + path))
                .andRespond(withSuccess(new ClassPathResource("opendota/" + fixture), MediaType.APPLICATION_JSON));
    }

    @Test
    void juntaPartidaHeroisEItensETraduzQuemMatou() {
        expect("/constants/heroes", "constants-heroes.json");
        expect("/matches/" + MATCH, "match-full.json");
        expect("/constants/items", "constants-items.json");

        MatchSnapshot snapshot = service.collect(MATCH).orElseThrow();

        assertThat(snapshot.match().matchId()).isEqualTo(MATCH);
        assertThat(snapshot.heroName(1)).isEqualTo("Anti-Mage");
        assertThat(snapshot.items()).isNotEmpty();
        MatchPlayer me = snapshot.match().player(34839344L).orElseThrow();
        assertThat(me.deathLog()).extracting(MatchPlayer.Death::killerHeroId)
                .as("Invoker não está nas constantes de teste: os dois assassinos vêm do kills_log dos adversários")
                .containsExactly(74, 1);
        server.verify();
    }

    @Test
    void pedeAnaliseDoReplay() {
        server.expect(requestTo(BASE + "/request/" + MATCH)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"job\":{\"jobId\":1}}", MediaType.APPLICATION_JSON));

        service.requestReplayParsing(MATCH);

        server.verify();
    }
}
