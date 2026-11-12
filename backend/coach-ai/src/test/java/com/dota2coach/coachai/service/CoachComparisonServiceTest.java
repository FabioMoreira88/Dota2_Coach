package com.dota2coach.coachai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dota2coach.coachai.client.CoachModelClient;
import com.dota2coach.coachai.client.CoachModelException;
import com.dota2coach.coachai.client.CoachReply;
import com.dota2coach.coachai.prompt.CoachPrompt;
import com.dota2coach.coachai.service.CoachComparisonService.Entry;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CoachComparisonServiceTest {

    private static final CoachPrompt PROMPT = new CoachPrompt("s", "u");

    private static CoachModelClient ok(String name, long in, long out) {
        return new CoachModelClient() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public CoachReply generate(CoachPrompt prompt) {
                return new CoachReply("texto de " + name, name + "-model", in, out, Duration.ofMillis(1500));
            }
        };
    }

    private static CoachModelClient failing(String name) {
        return new CoachModelClient() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public CoachReply generate(CoachPrompt prompt) {
                throw new CoachModelException("sem conexão | timeout");
            }
        };
    }

    private final List<CoachModelClient> clients =
            List.of(ok("claude", 1_000_000, 100_000), ok("llama", 5000, 800), failing("qwen"));
    private final Map<String, ProviderPricing> pricing = Map.of("claude", new ProviderPricing(4, 20));

    @Test
    void rodaTodosRotulaComLetrasECalculaOCusto() {
        List<Entry> entries = new CoachComparisonService(new Random(42)).compare(PROMPT, clients, pricing);

        assertThat(entries).extracting(Entry::label).containsExactly("A", "B", "C");
        assertThat(entries).extracting(Entry::provider).containsExactlyInAnyOrder("claude", "llama", "qwen");
        Entry claude = entries.stream().filter(e -> e.provider().equals("claude")).findFirst().orElseThrow();
        assertThat(claude.costUsd()).isEqualTo(6.0);
        Entry qwen = entries.stream().filter(e -> e.provider().equals("qwen")).findFirst().orElseThrow();
        assertThat(qwen.succeeded()).isFalse();
        assertThat(qwen.error()).contains("timeout");
    }

    @Test
    void ordemDependeDoEmbaralhamento() {
        List<String> first = new CoachComparisonService(new Random(1)).compare(PROMPT, clients, pricing)
                .stream().map(Entry::provider).toList();
        boolean changed = false;
        for (int seed = 2; seed < 20 && !changed; seed++) {
            List<String> other = new CoachComparisonService(new Random(seed)).compare(PROMPT, clients, pricing)
                    .stream().map(Entry::provider).toList();
            changed = !other.equals(first);
        }
        assertThat(changed).isTrue();
    }

    @Test
    void textoAsCegasNaoMostraOProvedorEOGabaritoMostra() {
        List<Entry> entries = new CoachComparisonService(new Random(42)).compare(PROMPT, clients, pricing);
        ComparisonReportRenderer renderer = new ComparisonReportRenderer(ZoneId.of("UTC"));

        String blind = renderer.blind(entries, "raio-x-1.md", Instant.parse("2026-11-12T10:00:00Z"));
        String key = renderer.key(entries);

        assertThat(blind).contains("## Texto A", "## Texto C", "texto de claude", "12/11/2026 10:00",
                "_Este modelo falhou");
        assertThat(blind).doesNotContain("claude-model", "llama-model", "qwen");
        assertThat(key).contains("claude-model", "1.5 s", "US$ 6.0000", "falhou: sem conexão \\| timeout");
    }
}
