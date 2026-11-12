package com.dota2coach.collector.client.opendota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;

import com.dota2coach.collector.config.OpenDotaProperties;
import com.dota2coach.collector.dto.opendota.OpenDotaHeroStatsResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaItemPopularityResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaMatchResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaPlayerHeroResponse;
import com.dota2coach.collector.support.RequestRateLimiter;
import com.dota2coach.core.exception.ExternalServiceUnavailableException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenDotaClientTest {

    private static final String BASE = "https://api.opendota.test/api";

    private final List<Duration> backoffs = new ArrayList<>();

    private OpenDotaClient client(RestClient.Builder builder, String apiKey) {
        OpenDotaProperties props = new OpenDotaProperties(
                BASE, apiKey, 6000, 3, Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(30),
                Duration.ofMinutes(15));
        RequestRateLimiter noLimit = new RequestRateLimiter(6000, () -> 0L, d -> { });
        return new OpenDotaClient(builder.baseUrl(BASE).build(), props, noLimit, backoffs::add);
    }

    @Test
    void leHeroesAceitandoHeroIdComoTextoOuNumero() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/players/34839344/heroes"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ClassPathResource("opendota/heroes.json"), MediaType.APPLICATION_JSON));

        List<OpenDotaPlayerHeroResponse> heroes = client(builder, "").getHeroes(34839344L);

        assertThat(heroes).hasSize(4);
        assertThat(heroes.get(1).heroId()).isEqualTo(35);
        server.verify();
    }

    @Test
    void tentaDeNovoComEsperaCrescenteEm429E5xx() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/players/1/wl")).andRespond(withTooManyRequests());
        server.expect(requestTo(BASE + "/players/1/wl")).andRespond(withServerError());
        server.expect(requestTo(BASE + "/players/1/wl"))
                .andRespond(withSuccess("{\"win\":3,\"lose\":2}", MediaType.APPLICATION_JSON));

        var wl = client(builder, "").getWinLoss(1L);

        assertThat(wl.win()).isEqualTo(3);
        assertThat(backoffs).containsExactly(Duration.ofSeconds(1), Duration.ofSeconds(2));
        server.verify();
    }

    @Test
    void desisteDepoisDoMaximoDeTentativas() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(times(3), requestTo(BASE + "/players/1/wl")).andRespond(withServerError());

        OpenDotaClient client = client(builder, "");

        assertThatThrownBy(() -> client.getWinLoss(1L))
                .isInstanceOf(ExternalServiceUnavailableException.class);
        server.verify();
    }

    @Test
    void enviaApiKeyQuandoConfigurada() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/players/1/wl?api_key=chave"))
                .andRespond(withSuccess("{\"win\":0,\"lose\":0}", MediaType.APPLICATION_JSON));

        client(builder, "chave").getWinLoss(1L);

        server.verify();
    }

    @Test
    void buscaPartidasPedindoOsCamposDoProject() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(allOf(
                        containsString(BASE + "/players/34839344/matches?"),
                        containsString("project=match_id"),
                        containsString("project=lane_role"),
                        containsString("project=item_neutral"))))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new ClassPathResource("opendota/matches.json"), MediaType.APPLICATION_JSON));

        List<OpenDotaMatchResponse> matches = client(builder, "").getMatches(34839344L);

        assertThat(matches).hasSize(4);
        assertThat(matches.get(0).laneRole()).isEqualTo(3);
        assertThat(matches.get(1).version()).isNull();
        server.verify();
    }

    @Test
    void leHeroStatsComChavesPorFaixaEmNumeroOuTexto() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/heroStats"))
                .andRespond(withSuccess(new ClassPathResource("opendota/hero-stats.json"), MediaType.APPLICATION_JSON));

        List<OpenDotaHeroStatsResponse> stats = client(builder, "").getHeroStats();

        assertThat(stats).hasSize(2);
        assertThat(stats.get(0).bracketPicks()).containsEntry(5, 5000).containsEntry(6, 6000);
        assertThat(stats.get(0).bracketWins()).containsEntry(6, 3000);
        assertThat(stats.get(0).proPick()).isEqualTo(12);
        assertThat(stats.get(1).bracketPicks()).containsEntry(1, 0).containsEntry(5, 4000);
        server.verify();
    }

    @Test
    void leConstantesEPopularidadeDeItens() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/constants/heroes")).andRespond(
                withSuccess(new ClassPathResource("opendota/constants-heroes.json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/constants/items")).andRespond(
                withSuccess(new ClassPathResource("opendota/constants-items.json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/heroes/35/itemPopularity")).andRespond(
                withSuccess(new ClassPathResource("opendota/item-popularity.json"), MediaType.APPLICATION_JSON));
        OpenDotaClient client = client(builder, "");

        assertThat(client.getHeroConstants().get("84").localizedName()).isEqualTo("Ogre Magi");
        assertThat(client.getItemConstants().get("blink").displayName()).isEqualTo("Blink Dagger");
        OpenDotaItemPopularityResponse popularity = client.getItemPopularity(35);
        assertThat(popularity.midGameItems()).containsEntry(166, 400);
        server.verify();
    }

    @Test
    void pedeAnaliseDoReplayComPost() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE + "/request/9083198111"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"job\":{\"jobId\":4242}}", MediaType.APPLICATION_JSON));

        var response = client(builder, "").requestParse(9083198111L);

        assertThat(response.job().jobId()).isEqualTo(4242L);
        server.verify();
    }
}
