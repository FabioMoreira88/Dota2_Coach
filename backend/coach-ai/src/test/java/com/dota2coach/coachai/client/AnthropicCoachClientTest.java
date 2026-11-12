package com.dota2coach.coachai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.services.blocking.MessageService;
import com.dota2coach.coachai.prompt.CoachPrompt;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnthropicCoachClientTest {

    private static final CoachPrompt PROMPT = new CoachPrompt("sistema", "raio-x");

    private final AnthropicClient anthropic = mock(AnthropicClient.class);
    private final MessageService messages = mock(MessageService.class);
    private AnthropicCoachClient client;

    @BeforeEach
    void setUp() {
        when(anthropic.messages()).thenReturn(messages);
        client = new AnthropicCoachClient("claude", anthropic, "claude-opus-5-5", "high",
                Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC));
    }

    private static Message message(String stopReason, String contentJson) throws Exception {
        String json = """
                {"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5-5",
                 "content":%s,"stop_reason":"%s","stop_sequence":null,
                 "usage":{"input_tokens":3000,"output_tokens":900}}
                """.formatted(contentJson, stopReason);
        return ObjectMappers.jsonMapper().readValue(json, Message.class);
    }

    @Test
    void juntaOsBlocosDeTextoEInformaOsTokens() throws Exception {
        when(messages.create(any(MessageCreateParams.class))).thenReturn(message("end_turn",
                "[{\"type\":\"thinking\",\"thinking\":\"...\",\"signature\":\"x\"},"
                        + "{\"type\":\"text\",\"text\":\"## Foco 1\"},{\"type\":\"text\",\"text\":\"## Foco 2\"}]"));

        CoachReply reply = client.generate(PROMPT);

        assertThat(reply.text()).isEqualTo("## Foco 1\n\n## Foco 2");
        assertThat(reply.model()).isEqualTo("claude-opus-5-5");
        assertThat(reply.inputTokens()).isEqualTo(3000);
        assertThat(reply.outputTokens()).isEqualTo(900);
    }

    @Test
    void recusaViraErro() throws Exception {
        when(messages.create(any(MessageCreateParams.class))).thenReturn(message("refusal", "[]"));

        assertThatThrownBy(() -> client.generate(PROMPT))
                .isInstanceOf(CoachModelException.class)
                .hasMessageContaining("recusou");
    }

    @Test
    void parametrosLigamOFallbackEUsamOModeloConfigurado() {
        MessageCreateParams params = client.params(PROMPT);

        assertThat(params.model().asString()).isEqualTo("claude-opus-5-5");
        assertThat(params.maxTokens()).isEqualTo(AnthropicCoachClient.MAX_TOKENS);
        assertThat(params._additionalHeaders().values("anthropic-beta"))
                .containsExactly(AnthropicCoachClient.FALLBACK_BETA);
        assertThat(params._additionalBodyProperties()).containsKey("fallbacks");
    }
}
