package com.dota2coach.collector.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dota2coach.collector.client.opendota.OpenDotaClient;
import com.dota2coach.collector.config.OpenDotaProperties;
import com.dota2coach.collector.support.RequestRateLimiter;
import com.dota2coach.core.exception.PlayerNotFoundException;
import com.dota2coach.core.model.PlayerSummary;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PlayerDataServiceTest {

    private static final String BASE = "https://api.opendota.test/api";
    private static final long ACCOUNT = 34839344L;

    private MockRestServiceServer server;
    private PlayerDataService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        OpenDotaProperties props = new OpenDotaProperties(BASE, "", 6000, 1, null, null, null, null);
        OpenDotaClient client = new OpenDotaClient(
                builder.build(), props, new RequestRateLimiter(6000, () -> 0L, d -> { }), d -> { });
        service = new PlayerDataService(client, Caffeine.newBuilder().<Long, PlayerSummary>build());
    }

    private void expectFullProfile() {
        server.expect(requestTo(BASE + "/players/" + ACCOUNT))
                .andRespond(withSuccess(new ClassPathResource("opendota/player.json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/players/" + ACCOUNT + "/wl"))
                .andRespond(withSuccess(new ClassPathResource("opendota/wl.json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/players/" + ACCOUNT + "/heroes"))
                .andRespond(withSuccess(new ClassPathResource("opendota/heroes.json"), MediaType.APPLICATION_JSON));
    }

    @Test
    void montaResumoComHeroisMaisJogadosEmOrdem() {
        expectFullProfile();

        PlayerSummary summary = service.getSummary(ACCOUNT);

        assertThat(summary.personaName()).isEqualTo("Jogador Teste");
        assertThat(summary.rankTier()).isEqualTo(53);
        assertThat(summary.totalMatches()).isEqualTo(8824);
        assertThat(summary.winRate()).isBetween(0.496, 0.497);
        assertThat(summary.topHeroes()).extracting("heroId").containsExactly(14, 35, 84);
        assertThat(summary.steamId().steamId64()).isEqualTo(76_561_197_995_105_072L);
    }

    @Test
    void segundaConsultaVemDoCacheSemChamarAOpenDota() {
        expectFullProfile();

        service.getSummary(ACCOUNT);
        service.getSummary(ACCOUNT);

        server.verify();
    }

    @Test
    void contaSemPerfilNaOpenDotaViraJogadorNaoEncontrado() {
        server.expect(requestTo(BASE + "/players/1"))
                .andRespond(withSuccess("{\"profile\":null,\"rank_tier\":null}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.getSummary(1L)).isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void semPartidasIndicaDadosPublicosDesligados() {
        server.expect(requestTo(BASE + "/players/" + ACCOUNT))
                .andRespond(withSuccess(new ClassPathResource("opendota/player.json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/players/" + ACCOUNT + "/wl"))
                .andRespond(withSuccess("{\"win\":0,\"lose\":0}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/players/" + ACCOUNT + "/heroes"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(service.getSummary(ACCOUNT).hasPublicMatchData()).isFalse();
    }
}
