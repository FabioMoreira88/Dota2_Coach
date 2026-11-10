package com.dota2coach.collector.client.steam;

import com.dota2coach.core.exception.ExternalServiceUnavailableException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Pergunta à própria Steam se uma asserção OpenID 2.0 é autêntica (modo check_authentication).
 *
 * <p>É o passo que impede alguém de forjar a volta do login: só a Steam sabe se assinou aqueles
 * parâmetros.
 */
public class SteamOpenIdClient {

    private final RestClient restClient;
    private final String endpoint;

    public SteamOpenIdClient(RestClient restClient, String endpoint) {
        this.restClient = restClient;
        this.endpoint = endpoint;
    }

    /** Devolve true só se a Steam responder {@code is_valid:true}. */
    public boolean isAssertionValid(Map<String, String> openIdParams) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        openIdParams.forEach(form::add);
        form.set("openid.mode", "check_authentication");
        try {
            String body = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            return body != null && body.lines().map(String::trim).anyMatch("is_valid:true"::equals);
        } catch (RestClientException e) {
            throw new ExternalServiceUnavailableException("Steam", e);
        }
    }
}
