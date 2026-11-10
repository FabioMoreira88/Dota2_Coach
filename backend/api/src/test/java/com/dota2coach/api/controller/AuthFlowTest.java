package com.dota2coach.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dota2coach.api.config.SecurityConfig;
import com.dota2coach.api.mapper.PlayerSummaryMapper;
import com.dota2coach.api.security.SteamOpenIdService;
import com.dota2coach.collector.client.steam.SteamOpenIdClient;
import com.dota2coach.collector.config.SteamProperties;
import com.dota2coach.collector.service.PlayerDataService;
import com.dota2coach.core.model.PlayerSummary;
import com.dota2coach.core.model.SteamId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest({AuthController.class, MeController.class})
@Import({SecurityConfig.class, SteamOpenIdService.class, PlayerSummaryMapper.class})
@EnableConfigurationProperties(SteamProperties.class)
class AuthFlowTest {

    private static final String CLAIMED = "https://steamcommunity.com/openid/id/76561197995105072";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SteamOpenIdClient steamOpenIdClient;

    @MockitoBean
    private PlayerDataService playerDataService;

    private String stateFrom(MvcResult login) {
        String location = login.getResponse().getHeader("Location");
        String marker = "state%3D";
        String tail = location.substring(location.indexOf(marker) + marker.length());
        int end = tail.indexOf('&');
        return end < 0 ? tail : tail.substring(0, end);
    }

    private MvcResult callback(MockHttpSession session, String state) throws Exception {
        return mvc.perform(get("/api/v1/auth/steam/callback")
                        .session(session)
                        .param("state", state)
                        .param("openid.ns", "http://specs.openid.net/auth/2.0")
                        .param("openid.mode", "id_res")
                        .param("openid.op_endpoint", "https://steamcommunity.com/openid/login")
                        .param("openid.claimed_id", CLAIMED)
                        .param("openid.identity", CLAIMED)
                        .param("openid.return_to", "http://localhost:8080/api/v1/auth/steam/callback?state=" + state)
                        .param("openid.sig", "x"))
                .andReturn();
    }

    @Test
    void semLoginMeDa401EmJson() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void loginCompletoDaAcessoAoMe() throws Exception {
        when(steamOpenIdClient.isAssertionValid(anyMap())).thenReturn(true);
        when(playerDataService.getSummary(34839344L)).thenReturn(new PlayerSummary(
                SteamId.fromAccountId(34839344L), "Jogador Teste", null, 53, 1, 0, List.of()));
        MockHttpSession session = new MockHttpSession();

        MvcResult login = mvc.perform(get("/api/v1/auth/steam/login").session(session))
                .andExpect(status().isFound())
                .andReturn();
        String sessionIdBefore = session.getId();
        MvcResult back = callback(session, stateFrom(login));

        assertThat(back.getResponse().getHeader("Location")).isEqualTo("http://localhost:3000/?login=ok");
        assertThat(session.getId()).isNotEqualTo(sessionIdBefore);
        mvc.perform(get("/api/v1/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steamId64").value("76561197995105072"))
                .andExpect(jsonPath("$.profile.personaName").value("Jogador Teste"))
                .andExpect(jsonPath("$.profile.rankTier").value(53));
    }

    @Test
    void callbackSemOLoginTerComecadoNaoAutentica() throws Exception {
        when(steamOpenIdClient.isAssertionValid(anyMap())).thenReturn(true);
        MockHttpSession session = new MockHttpSession();

        MvcResult back = callback(session, "state-inventado");

        assertThat(back.getResponse().getHeader("Location")).isEqualTo("http://localhost:3000/?login=erro");
        mvc.perform(get("/api/v1/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutExigeCsrfEEncerraASessao() throws Exception {
        when(steamOpenIdClient.isAssertionValid(anyMap())).thenReturn(true);
        MockHttpSession session = new MockHttpSession();
        MvcResult login = mvc.perform(get("/api/v1/auth/steam/login").session(session)).andReturn();
        callback(session, stateFrom(login));

        mvc.perform(post("/api/v1/auth/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void respostasTemCabecalhosDeSeguranca() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Content-Security-Policy"));
    }
}
