package com.dota2coach.api.cli;

import com.dota2coach.analysis.metric.ProfileXRay;
import com.dota2coach.analysis.metric.XRayCalculator;
import com.dota2coach.analysis.report.XRayMarkdownRenderer;
import com.dota2coach.api.config.CoachConfig.CoachClientFactory;
import com.dota2coach.api.config.CoachProperties;
import com.dota2coach.api.config.RaioXProperties;
import com.dota2coach.coachai.client.CoachReply;
import com.dota2coach.coachai.prompt.CoachPromptFactory;
import com.dota2coach.collector.service.ProfileSnapshotService;
import com.dota2coach.core.model.CoachDiagnosis;
import com.dota2coach.core.model.ProfileSnapshot;
import com.dota2coach.core.model.XRayReport;
import com.dota2coach.core.service.XRayHistoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Gera o relatório Raio-X em Markdown ao subir a aplicação com {@code --dota2coach.raio-x.account-id=...}, salva no
 * histórico e, com {@code --dota2coach.raio-x.diagnose=true}, gera e salva o diagnóstico do coach.
 * Com {@code --spring.main.web-application-type=none} a aplicação termina ao final.
 */
@Component
@ConditionalOnProperty(prefix = "dota2coach.raio-x", name = "account-id")
public class RaioXCommand implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RaioXCommand.class);

    private final ProfileSnapshotService snapshotService;
    private final XRayCalculator calculator;
    private final XRayMarkdownRenderer renderer;
    private final RaioXProperties properties;
    private final XRayHistoryService history;
    private final ObjectMapper objectMapper;
    private final CoachProperties coachProperties;
    private final CoachClientFactory clientFactory;
    private final Clock clock;

    public RaioXCommand(
            ProfileSnapshotService snapshotService,
            XRayCalculator calculator,
            XRayMarkdownRenderer renderer,
            RaioXProperties properties,
            XRayHistoryService history,
            ObjectMapper objectMapper,
            CoachProperties coachProperties,
            CoachClientFactory clientFactory,
            Clock clock) {
        this.snapshotService = snapshotService;
        this.calculator = calculator;
        this.renderer = renderer;
        this.properties = properties;
        this.history = history;
        this.objectMapper = objectMapper;
        this.coachProperties = coachProperties;
        this.clientFactory = clientFactory;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        long accountId = properties.accountId();
        if (properties.position() == null) {
            log.warn("Raio-X: posição principal não informada. Use --dota2coach.raio-x.position=mid (ou carry,"
                    + " offlane, soft-support, hard-support) para o coach focar na sua posição.");
        }
        log.info("Raio-X: coletando dados da conta {} na OpenDota...", accountId);
        ProfileSnapshot snapshot = snapshotService.collect(accountId);
        log.info("Raio-X: {} partida(s) encontrada(s).", snapshot.matches().size());

        if (properties.parseLimit() > 0) {
            List<Long> requested = snapshotService.requestReplayParsing(snapshot.matches(), properties.parseLimit());
            if (requested.isEmpty()) {
                log.info("Raio-X: nenhuma partida recente precisa de análise de replay.");
            } else {
                log.info("Raio-X: análise de replay pedida para {} partida(s). Rode de novo em uns 15 minutos para"
                        + " ver rotas e itens dessas partidas.", requested.size());
            }
        }

        ProfileXRay xRay = calculator.calculate(snapshot, properties.position());
        String markdown = renderer.render(xRay);
        Path output = write(Path.of(properties.resolvedOutput()), markdown);
        log.info("Raio-X: relatório salvo em {}", output);

        XRayReport report = history.saveReport(new XRayReport(accountId, properties.position(),
                xRay.collectedAt(), xRay.recentWindow(), toJson(xRay), markdown));
        log.info("Raio-X: guardado no histórico (Raio-X nº {} desta conta).", history.count(accountId));

        if (properties.diagnose()) {
            diagnose(report, output);
        }
    }

    private void diagnose(XRayReport report, Path reportFile) {
        String name = properties.coachProvider();
        CoachProperties.Provider provider = coachProperties.providers().get(name);
        if (provider == null) {
            throw new IllegalStateException("Provedor de IA '" + name + "' não está em dota2coach.coach.providers");
        }
        if (provider.apiKeyRequired() && !provider.hasApiKey()) {
            throw new IllegalStateException("Falta a chave do provedor '" + name + "' no .env");
        }
        log.info("Diagnóstico: gerando com {} (leva cerca de 1 minuto)...", name);
        CoachReply reply = clientFactory.apply(name, provider)
                .generate(CoachPromptFactory.fromXRayReport(report.getMarkdown()));
        BigDecimal cost = BigDecimal.valueOf(provider.pricing().cost(reply.inputTokens(), reply.outputTokens()))
                .setScale(4, RoundingMode.HALF_UP);
        history.saveDiagnosis(new CoachDiagnosis(report, name, reply.model(), clock.instant(),
                reply.inputTokens(), reply.outputTokens(), cost, reply.text()));
        Path file = reportFile.resolveSibling(reportFile.getFileName().toString().replace("raio-x-", "diagnostico-"));
        if (file.equals(reportFile)) {
            file = reportFile.resolveSibling("diagnostico-" + reportFile.getFileName());
        }
        write(file, reply.text() + "\n");
        log.info("Diagnóstico: salvo em {} e no histórico (custo estimado US$ {}).", file.toAbsolutePath().normalize(),
                cost.toPlainString());
    }

    private String toJson(ProfileXRay xRay) {
        try {
            return objectMapper.writeValueAsString(xRay);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível converter o Raio-X em JSON", e);
        }
    }

    private static Path write(Path path, String markdown) {
        Path output = path.toAbsolutePath().normalize();
        try {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(output, markdown, StandardCharsets.UTF_8);
            return output;
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar o arquivo em " + output, e);
        }
    }
}
