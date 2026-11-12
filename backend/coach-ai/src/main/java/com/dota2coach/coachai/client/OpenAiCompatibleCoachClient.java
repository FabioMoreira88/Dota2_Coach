package com.dota2coach.coachai.client;

import com.dota2coach.coachai.prompt.CoachPrompt;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Coach via modelos abertos servidos no formato de chat da OpenAI ({@code POST /chat/completions}). Serve para o
 * Ollama local e para serviços como Groq e OpenRouter. Não é usado para o Claude, que tem cliente próprio.
 */
public class OpenAiCompatibleCoachClient implements CoachModelClient {

    private final String name;
    private final RestClient restClient;
    private final String model;
    private final Clock clock;

    /** O {@code restClient} já vem com a URL base e, se houver, o cabeçalho de autorização. */
    public OpenAiCompatibleCoachClient(String name, RestClient restClient, String model, Clock clock) {
        this.name = name;
        this.restClient = restClient;
        this.model = model;
        this.clock = clock;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CoachReply generate(CoachPrompt prompt) {
        Instant start = clock.instant();
        ChatResponse response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ChatRequest(model, List.of(
                            new ChatMessage("system", prompt.system()),
                            new ChatMessage("user", prompt.user()))))
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (RestClientException e) {
            throw new CoachModelException(name + " falhou: " + e.getMessage(), e);
        }
        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().get(0).message() == null
                || response.choices().get(0).message().content() == null
                || response.choices().get(0).message().content().isBlank()) {
            throw new CoachModelException(name + " devolveu uma resposta sem texto");
        }
        Usage usage = response.usage() == null ? new Usage(0L, 0L) : response.usage();
        return new CoachReply(
                response.choices().get(0).message().content().strip(),
                response.model() == null ? model : response.model(),
                usage.promptTokens() == null ? 0 : usage.promptTokens(),
                usage.completionTokens() == null ? 0 : usage.completionTokens(),
                Duration.between(start, clock.instant()));
    }

    record ChatRequest(String model, List<ChatMessage> messages) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatMessage(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(String model, List<Choice> choices, Usage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(ChatMessage message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(
            @JsonProperty("prompt_tokens") Long promptTokens,
            @JsonProperty("completion_tokens") Long completionTokens) {}
}
