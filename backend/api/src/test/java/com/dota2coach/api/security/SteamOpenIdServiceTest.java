package com.dota2coach.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dota2coach.api.config.AppProperties;
import com.dota2coach.collector.client.steam.SteamOpenIdClient;
import com.dota2coach.collector.config.SteamProperties;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SteamOpenIdServiceTest {

    private static final String STATE = "estado-aleatorio";

    private SteamOpenIdClient client;
    private SteamOpenIdService service;

    @BeforeEach
    void setUp() {
        client = mock(SteamOpenIdClient.class);
        service = new SteamOpenIdService(client, new SteamProperties(null, ""),
                new AppProperties("http://localhost:8080/", "http://localhost:3000"));
    }

    private Map<String, String> validParams() {
        Map<String, String> p = new HashMap<>();
        p.put("openid.ns", "http://specs.openid.net/auth/2.0");
        p.put("openid.mode", "id_res");
        p.put("openid.op_endpoint", "https://steamcommunity.com/openid/login");
        p.put("openid.claimed_id", "https://steamcommunity.com/openid/id/76561197995105072");
        p.put("openid.identity", "https://steamcommunity.com/openid/id/76561197995105072");
        p.put("openid.return_to", "http://localhost:8080/api/v1/auth/steam/callback?state=" + STATE);
        p.put("openid.response_nonce", "2026-11-10T23:00:00Zabc");
        p.put("openid.assoc_handle", "1234567890");
        p.put("openid.signed", "signed,op_endpoint,claimed_id,identity,return_to,response_nonce,assoc_handle");
        p.put("openid.sig", "assinatura");
        return p;
    }

    @Test
    void urlDeLoginApontaParaASteamComReturnToEState() {
        String url = service.loginUrl(STATE);

        assertThat(url).startsWith("https://steamcommunity.com/openid/login?")
                .contains("openid.mode=checkid_setup")
                .contains("openid.realm=http://localhost:8080")
                .contains("state%3D" + STATE);
    }

    @Test
    void assercaoValidaDevolveOSteamId() {
        when(client.isAssertionValid(anyMap())).thenReturn(true);

        assertThat(service.verify(validParams(), STATE, STATE).steamId64())
                .isEqualTo(76_561_197_995_105_072L);
    }

    @Test
    void recusaStateDiferenteDaSessaoSemChamarASteam() {
        assertThatThrownBy(() -> service.verify(validParams(), "outro", STATE))
                .isInstanceOf(SteamLoginException.class);
        assertThatThrownBy(() -> service.verify(validParams(), STATE, null))
                .isInstanceOf(SteamLoginException.class);
        verify(client, never()).isAssertionValid(anyMap());
    }

    @Test
    void recusaOpEndpointQueNaoEDaSteam() {
        Map<String, String> p = validParams();
        p.put("openid.op_endpoint", "https://steamcommunity.com.evil.example/openid/login");

        assertThatThrownBy(() -> service.verify(p, STATE, STATE)).isInstanceOf(SteamLoginException.class);
    }

    @Test
    void recusaReturnToDeOutroSite() {
        Map<String, String> p = validParams();
        p.put("openid.return_to", "https://evil.example/api/v1/auth/steam/callback?state=" + STATE);

        assertThatThrownBy(() -> service.verify(p, STATE, STATE)).isInstanceOf(SteamLoginException.class);
    }

    @Test
    void recusaClaimedIdForaDoFormatoOuDiferenteDaIdentity() {
        Map<String, String> p = validParams();
        p.put("openid.claimed_id", "https://steamcommunity.com/openid/id/123");
        p.put("openid.identity", "https://steamcommunity.com/openid/id/123");
        assertThatThrownBy(() -> service.verify(p, STATE, STATE)).isInstanceOf(SteamLoginException.class);

        Map<String, String> q = validParams();
        q.put("openid.identity", "https://steamcommunity.com/openid/id/76561197960265729");
        assertThatThrownBy(() -> service.verify(q, STATE, STATE)).isInstanceOf(SteamLoginException.class);
    }

    @Test
    void recusaQuandoASteamNaoConfirma() {
        when(client.isAssertionValid(anyMap())).thenReturn(false);

        assertThatThrownBy(() -> service.verify(validParams(), STATE, STATE))
                .isInstanceOf(SteamLoginException.class);
    }
}
