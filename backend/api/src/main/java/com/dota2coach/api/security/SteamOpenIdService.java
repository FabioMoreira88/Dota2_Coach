package com.dota2coach.api.security;

import com.dota2coach.api.config.AppProperties;
import com.dota2coach.collector.client.steam.SteamOpenIdClient;
import com.dota2coach.collector.config.SteamProperties;
import com.dota2coach.core.model.SteamId;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Login pela Steam via OpenID 2.0.
 *
 * <p>O app nunca vê senha nem Steam Guard: o jogador loga no site da Steam, que devolve uma
 * asserção assinada com o SteamID. Aqui essa asserção é conferida campo a campo e depois
 * confirmada com a própria Steam.
 */
@Service
public class SteamOpenIdService {

    static final String CALLBACK_PATH = "/api/v1/auth/steam/callback";
    private static final String OPENID_NS = "http://specs.openid.net/auth/2.0";
    private static final String IDENTIFIER_SELECT = "http://specs.openid.net/auth/2.0/identifier_select";
    private static final Pattern CLAIMED_ID =
            Pattern.compile("^https://steamcommunity\\.com/openid/id/(\\d{17})$");

    private final SteamOpenIdClient client;
    private final SteamProperties steamProperties;
    private final AppProperties appProperties;
    private final SecureRandom random = new SecureRandom();

    public SteamOpenIdService(
            SteamOpenIdClient client, SteamProperties steamProperties, AppProperties appProperties) {
        this.client = client;
        this.steamProperties = steamProperties;
        this.appProperties = appProperties;
    }

    /** Valor aleatório guardado na sessão e amarrado ao return_to, contra login forjado (CSRF). */
    public String newState() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String returnTo(String state) {
        return UriComponentsBuilder.fromUriString(appProperties.baseUrl() + CALLBACK_PATH)
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    /** URL do site da Steam para onde o navegador é mandado. */
    public String loginUrl(String state) {
        return UriComponentsBuilder.fromUriString(steamProperties.openIdEndpoint())
                .queryParam("openid.ns", OPENID_NS)
                .queryParam("openid.mode", "checkid_setup")
                .queryParam("openid.return_to", returnTo(state))
                .queryParam("openid.realm", appProperties.baseUrl())
                .queryParam("openid.identity", IDENTIFIER_SELECT)
                .queryParam("openid.claimed_id", IDENTIFIER_SELECT)
                .encode()
                .build()
                .toUriString();
    }

    /**
     * Confere a volta do login e devolve o SteamID autenticado.
     *
     * @param params parâmetros {@code openid.*} recebidos no callback
     * @param receivedState valor de {@code state} recebido no callback
     * @param expectedState valor guardado na sessão quando o login começou
     * @throws SteamLoginException se qualquer verificação falhar
     */
    public SteamId verify(Map<String, String> params, String receivedState, String expectedState) {
        if (expectedState == null || !constantTimeEquals(expectedState, receivedState)) {
            throw new SteamLoginException("state ausente ou diferente da sessão");
        }
        require("id_res".equals(params.get("openid.mode")), "openid.mode inválido");
        require(OPENID_NS.equals(params.get("openid.ns")), "openid.ns inválido");
        require(steamProperties.openIdEndpoint().equals(params.get("openid.op_endpoint")),
                "openid.op_endpoint não é a Steam");
        require(returnTo(expectedState).equals(params.get("openid.return_to")),
                "openid.return_to diferente do esperado");

        String claimedId = params.get("openid.claimed_id");
        require(claimedId != null && claimedId.equals(params.get("openid.identity")),
                "claimed_id e identity diferentes");
        Matcher matcher = CLAIMED_ID.matcher(claimedId);
        require(matcher.matches(), "claimed_id fora do formato da Steam");

        require(client.isAssertionValid(params), "a Steam não confirmou a asserção");
        return new SteamId(Long.parseLong(matcher.group(1)));
    }

    private static void require(boolean condition, String reason) {
        if (!condition) {
            throw new SteamLoginException(reason);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return b != null && java.security.MessageDigest.isEqual(
                a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
