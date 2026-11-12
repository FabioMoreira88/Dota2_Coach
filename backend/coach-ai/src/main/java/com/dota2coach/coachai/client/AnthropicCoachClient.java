package com.dota2coach.coachai.client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.dota2coach.coachai.prompt.CoachPrompt;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Coach via Claude, com o SDK oficial da Anthropic.
 *
 * <p>Usa thinking adaptativo (padrão do modelo) e liga o fallback do lado do servidor: se um classificador de
 * segurança recusar o pedido, a API tenta de novo com outro modelo em vez de devolver só a recusa.
 */
public class AnthropicCoachClient implements CoachModelClient {

    static final long MAX_TOKENS = 16_000L;
    static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";

    private final String name;
    private final AnthropicClient client;
    private final String model;
    private final OutputConfig.Effort effort;
    private final Clock clock;

    public AnthropicCoachClient(String name, AnthropicClient client, String model, String effort, Clock clock) {
        this.name = name;
        this.client = client;
        this.model = model;
        this.effort = OutputConfig.Effort.of(effort);
        this.clock = clock;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CoachReply generate(CoachPrompt prompt) {
        Instant start = clock.instant();
        Message message;
        try {
            message = client.messages().create(params(prompt));
        } catch (AnthropicException e) {
            throw new CoachModelException("Claude falhou: " + e.getMessage(), e);
        }
        if (message.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
            throw new CoachModelException("Claude recusou o pedido");
        }
        String text = message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .collect(Collectors.joining("\n\n"))
                .strip();
        if (text.isEmpty()) {
            throw new CoachModelException("Claude devolveu uma resposta sem texto");
        }
        return new CoachReply(text, message.model().asString(), message.usage().inputTokens(),
                message.usage().outputTokens(), Duration.between(start, clock.instant()));
    }

    MessageCreateParams params(CoachPrompt prompt) {
        return MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(prompt.system())
                .addUserMessage(prompt.user())
                .outputConfig(OutputConfig.builder().effort(effort).build())
                .putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .build();
    }
}
