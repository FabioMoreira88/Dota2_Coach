package com.dota2coach.collector.client.opendota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;

import com.dota2coach.collector.config.OpenDotaProperties;
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
                BASE, apiKey, 6000, 3, Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofMinutes(15));
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
}
