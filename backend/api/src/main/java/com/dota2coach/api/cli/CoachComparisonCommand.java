package com.dota2coach.api.cli;

import com.dota2coach.api.config.CoachConfig.CoachClientFactory;
import com.dota2coach.api.config.CoachProperties;
import com.dota2coach.coachai.client.CoachModelClient;
import com.dota2coach.coachai.prompt.CoachPrompt;
import com.dota2coach.coachai.prompt.CoachPromptFactory;
import com.dota2coach.coachai.service.CoachComparisonService;
import com.dota2coach.coachai.service.CoachComparisonService.Entry;
import com.dota2coach.coachai.service.ComparisonReportRenderer;
import com.dota2coach.coachai.service.ProviderPricing;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Comparativo de IA às cegas: manda o Raio-X de uma conta a cada modelo configurado e salva os textos sem o nome
 * do modelo, mais um gabarito separado. Roda com {@code --dota2coach.coach.account-id=...}.
 */
@Component
@ConditionalOnProperty(prefix = "dota2coach.coach", name = "account-id")
public class CoachComparisonCommand implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CoachComparisonCommand.class);

    private final CoachProperties properties;
    private final CoachClientFactory clientFactory;
    private final CoachComparisonService comparisonService;
    private final ComparisonReportRenderer renderer;
    private final Clock clock;

    public CoachComparisonCommand(
            CoachProperties properties,
            CoachClientFactory clientFactory,
            CoachComparisonService comparisonService,
            ComparisonReportRenderer renderer,
            Clock clock) {
        this.properties = properties;
        this.clientFactory = clientFactory;
        this.comparisonService = comparisonService;
        this.renderer = renderer;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path report = Path.of(properties.resolvedReport()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(report)) {
            throw new IllegalStateException("Raio-X não encontrado em " + report
                    + ". Gere o Raio-X antes com --dota2coach.raio-x.account-id=" + properties.accountId());
        }
        CoachPrompt prompt = CoachPromptFactory.fromXRayReport(read(report));

        List<CoachModelClient> clients = new ArrayList<>();
        Map<String, ProviderPricing> pricing = new HashMap<>();
        properties.providers().forEach((name, provider) -> {
            if (!provider.enabled()) {
                log.info("Comparativo de IA: {} está desligado na configuração.", name);
            } else if (provider.apiKeyRequired() && !provider.hasApiKey()) {
                log.warn("Comparativo de IA: {} ficou de fora porque a chave não está no .env.", name);
            } else {
                clients.add(clientFactory.apply(name, provider));
                pricing.put(name, provider.pricing());
            }
        });
        if (clients.isEmpty()) {
            throw new IllegalStateException("Nenhum modelo disponível para o comparativo. Confira as chaves no .env.");
        }

        List<Entry> entries = comparisonService.compare(prompt, clients, pricing);
        String suffix = String.valueOf(properties.accountId());
        Path dir = Path.of(properties.outputDir()).toAbsolutePath().normalize();
        Path blind = write(dir.resolve("comparativo-ia-" + suffix + ".md"),
                renderer.blind(entries, report.getFileName().toString(), clock.instant()));
        Path key = write(dir.resolve("gabarito-ia-" + suffix + ".md"), renderer.key(entries));
        log.info("Comparativo de IA: textos em {}. Dê as notas antes de abrir o gabarito ({}).", blind, key);
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + file, e);
        }
    }

    private static Path write(Path file, String markdown) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, markdown, StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar " + file, e);
        }
    }
}
