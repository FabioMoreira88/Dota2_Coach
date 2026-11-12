package com.dota2coach.coachai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dota2coach.coachai.prompt.CoachPrompt;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiCompatibleCoachClientTest {

    private static final CoachPrompt PROMPT = new CoachPrompt("sistema", "raio-x");

    private MockRestServiceServer server;
    private OpenAiCompatibleCoachClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.groq.com/openai/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenAiCompatibleCoachClient("llama-70b-groq", builder.build(), "llama-3.3-70b-versatile",
                Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void mandaSystemEUserELeOTextoEOsTokens() {
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"model":"llama-3.3-70b-versatile","messages":[
                          {"role":"system","content":"sistema"},{"role":"user","content":"raio-x"}]}
                        """))
                .andRespond(withSuccess("""
                        {"model":"llama-3.3-70b-versatile","choices":[{"index":0,
                          "message":{"role":"assistant","content":"  ## Foco 1  "}}],
                         "usage":{"prompt_tokens":2500,"completion_tokens":700}}
                        """, MediaType.APPLICATION_JSON));

        CoachReply reply = client.generate(PROMPT);

        assertThat(reply.text()).isEqualTo("## Foco 1");
        assertThat(reply.inputTokens()).isEqualTo(2500);
        assertThat(reply.outputTokens()).isEqualTo(700);
        server.verify();
    }

    @Test
    void semUsageContaZeroTokens() {
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}",
                        MediaType.APPLICATION_JSON));

        CoachReply reply = client.generate(PROMPT);

        assertThat(reply.model()).isEqualTo("llama-3.3-70b-versatile");
        assertThat(reply.inputTokens()).isZero();
    }

    @Test
    void erroDoServidorViraCoachModelException() {
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions")).andRespond(withServerError());

        assertThatThrownBy(() -> client.generate(PROMPT)).isInstanceOf(CoachModelException.class);
    }

    @Test
    void respostaSemTextoViraErro() {
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generate(PROMPT))
                .isInstanceOf(CoachModelException.class)
                .hasMessageContaining("sem texto");
    }
}
