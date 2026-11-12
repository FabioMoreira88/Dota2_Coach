package com.dota2coach.api.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dota2coach.api.config.CoachProperties;
import com.dota2coach.api.config.CoachProperties.Provider;
import com.dota2coach.coachai.client.CoachModelClient;
import com.dota2coach.coachai.client.CoachReply;
import com.dota2coach.coachai.enums.ProviderType;
import com.dota2coach.coachai.prompt.CoachPrompt;
import com.dota2coach.coachai.service.CoachComparisonService;
import com.dota2coach.coachai.service.ComparisonReportRenderer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;

class CoachComparisonCommandTest {

    private static final long ACCOUNT = 34839344L;

    @TempDir
    Path dir;

    private final List<String> built = new ArrayList<>();

    private CoachComparisonCommand command(Map<String, Provider> providers) {
        CoachProperties props = new CoachProperties(ACCOUNT, dir.resolve("raio-x-{accountId}.md").toString(),
                dir.toString(), ZoneId.of("UTC"), providers);
        return new CoachComparisonCommand(props, (name, provider) -> {
                    built.add(name);
                    return fake(name);
                },
                new CoachComparisonService(new Random(7)), new ComparisonReportRenderer(ZoneId.of("UTC")),
                Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC));
    }

    private static CoachModelClient fake(String name) {
        return new CoachModelClient() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public CoachReply generate(CoachPrompt prompt) {
                return new CoachReply("texto " + name, name, 10, 5, Duration.ofSeconds(1));
            }
        };
    }

    private static Provider provider(String apiKey, boolean enabled, boolean keyRequired) {
        return new Provider(ProviderType.OPENAI_COMPATIBLE, "http://x", apiKey, "m", enabled, keyRequired,
                null, null, null, null);
    }

    @Test
    void pulaProvedoresSemChaveOuDesligadosESalvaOsDoisArquivos() throws Exception {
        Files.writeString(dir.resolve("raio-x-34839344.md"), "# Raio-X de Heisenberg");
        Map<String, Provider> providers = new LinkedHashMap<>();
        providers.put("claude", provider("chave", true, true));
        providers.put("groq", provider("", true, true));
        providers.put("local", provider(null, false, false));
        providers.put("local-ligado", provider(null, true, false));

        command(providers).run(new DefaultApplicationArguments());

        assertThat(built).containsExactly("claude", "local-ligado");
        assertThat(Files.readString(dir.resolve("comparativo-ia-34839344.md"))).contains("## Texto B");
        assertThat(Files.readString(dir.resolve("gabarito-ia-34839344.md"))).contains("claude");
    }

    @Test
    void semRaioXExplicaComoGerar() {
        assertThatThrownBy(() -> command(Map.of()).run(new DefaultApplicationArguments()))
                .hasMessageContaining("--dota2coach.raio-x.account-id=34839344");
    }

    @Test
    void semNenhumModeloFalha() throws Exception {
        Files.writeString(dir.resolve("raio-x-34839344.md"), "# Raio-X");

        assertThatThrownBy(() -> command(Map.of("groq", provider(null, true, true)))
                        .run(new DefaultApplicationArguments()))
                .hasMessageContaining("Nenhum modelo");
    }
}
