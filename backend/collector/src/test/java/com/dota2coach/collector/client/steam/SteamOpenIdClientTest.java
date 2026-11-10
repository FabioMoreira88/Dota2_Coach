package com.dota2coach.collector.client.steam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SteamOpenIdClientTest {

    private static final String ENDPOINT = "https://steam.test/openid/login";

    private boolean check(String steamAnswer) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of(
                        "openid.mode", "check_authentication", "openid.sig", "abc")))
                .andRespond(withSuccess(steamAnswer, MediaType.TEXT_PLAIN));
        boolean valid = new SteamOpenIdClient(builder.build(), ENDPOINT)
                .isAssertionValid(Map.of("openid.mode", "id_res", "openid.sig", "abc"));
        server.verify();
        return valid;
    }

    @Test
    void validaQuandoASteamRespondeIsValidTrue() {
        assertThat(check("ns:http://specs.openid.net/auth/2.0\nis_valid:true\n")).isTrue();
    }

    @Test
    void recusaQuandoASteamRespondeIsValidFalse() {
        assertThat(check("ns:http://specs.openid.net/auth/2.0\nis_valid:false\n")).isFalse();
    }
}
