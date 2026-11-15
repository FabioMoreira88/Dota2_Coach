package com.dota2coach.api.cli;

import com.dota2coach.analysis.evolution.FocusGoalCheck;
import com.dota2coach.analysis.evolution.FocusGoalChecker;
import com.dota2coach.analysis.evolution.XRayEvolution;
import com.dota2coach.analysis.evolution.XRayEvolutionCalculator;
import com.dota2coach.analysis.metric.ProfileXRay;
import com.dota2coach.analysis.metric.XRayCalculator;
import com.dota2coach.analysis.report.XRayMarkdownRenderer;
import com.dota2coach.api.config.CoachConfig.CoachClientFactory;
import com.dota2coach.api.config.CoachProperties;
import com.dota2coach.api.config.RaioXProperties;
import com.dota2coach.coachai.client.CoachReply;
import com.dota2coach.coachai.prompt.CoachPromptFactory;
import com.dota2coach.coachai.prompt.FocusGoalBlock;
import com.dota2coach.collector.service.ProfileSnapshotService;
import com.dota2coach.core.model.CoachDiagnosis;
import com.dota2coach.core.model.FocusGoal;
import com.dota2coach.core.model.ProfileSnapshot;
import com.dota2coach.core.model.XRayReport;
import com.dota2coach.core.service.XRayHistoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
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
        XRayReport previous = history.latest(accountId).orElse(null);
        XRayEvolution evolution = evolution(previous, xRay);
        CoachDiagnosis previousDiagnosis = previous == null ? null : previousDiagnosis(previous);
        List<FocusGoalCheck> goals = previousDiagnosis == null || previousDiagnosis.getId() == null ? List.of()
                : FocusGoalChecker.checkXRay(xRay, history.goals(previousDiagnosis.getId()));
        String markdown = renderer.render(xRay, evolution, goals);
        Path output = write(Path.of(properties.resolvedOutput()), markdown);
        log.info("Raio-X: relatório salvo em {}", output);

        XRayReport report = history.saveReport(new XRayReport(accountId, properties.position(),
                xRay.collectedAt(), xRay.recentWindow(), toJson(xRay), markdown));
        log.info("Raio-X: guardado no histórico (Raio-X nº {} desta conta).", history.count(accountId));

        if (properties.diagnose()) {
            diagnose(report, xRay, output, previousDiagnosis == null ? null : previousDiagnosis.getText());
        }
    }

    /** Compara com o Raio-X anterior; sem anterior, ou com um formato antigo que não dá para ler, não compara. */
    private XRayEvolution evolution(XRayReport previous, ProfileXRay current) {
        if (previous == null) {
            log.info("Raio-X: primeiro Raio-X desta conta, sem comparação de evolução.");
            return null;
        }
        try {
            ProfileXRay before = objectMapper.readerFor(ProfileXRay.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(previous.getMetricsJson());
            return XRayEvolutionCalculator.compare(before, current);
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("Raio-X: não deu para ler o Raio-X anterior ({}); o relatório sai sem a seção de evolução.",
                    e.getMessage());
            return null;
        }
    }

    private CoachDiagnosis previousDiagnosis(XRayReport previous) {
        if (previous.getId() == null) {
            return null;
        }
        return history.diagnoses(previous.getId()).stream().findFirst().orElse(null);
    }

    private void diagnose(XRayReport report, ProfileXRay xRay, Path reportFile, String previousDiagnosis) {
        String name = properties.coachProvider();
        CoachProperties.Provider provider = coachProperties.providers().get(name);
        if (provider == null) {
            throw new IllegalStateException("Provedor de IA '" + name + "' não está em dota2coach.coach.providers");
        }
        if (provider.apiKeyRequired() && !provider.hasApiKey()) {
            throw new IllegalStateException("Falta a chave do provedor '" + name + "' no .env");
        }
        log.info("Diagnóstico: gerando com {} (leva cerca de 1 minuto)...", name);
        if (previousDiagnosis != null) {
            log.info("Diagnóstico: o coach vai conferir os focos do diagnóstico anterior.");
        }
        CoachReply reply = clientFactory.apply(name, provider)
                .generate(CoachPromptFactory.fromXRayReport(report.getMarkdown(), previousDiagnosis));
        BigDecimal cost = BigDecimal.valueOf(provider.pricing().cost(reply.inputTokens(), reply.outputTokens()))
                .setScale(4, RoundingMode.HALF_UP);
        FocusGoalBlock.Parsed parsed = FocusGoalBlock.parse(reply.text());
        CoachDiagnosis diagnosis = history.saveDiagnosis(new CoachDiagnosis(report, name, reply.model(),
                clock.instant(), reply.inputTokens(), reply.outputTokens(), cost, parsed.text()));
        saveGoals(diagnosis, xRay, parsed);
        Path file = reportFile.resolveSibling(reportFile.getFileName().toString().replace("raio-x-", "diagnostico-"));
        if (file.equals(reportFile)) {
            file = reportFile.resolveSibling("diagnostico-" + reportFile.getFileName());
        }
        write(file, parsed.text() + "\n");
        log.info("Diagnóstico: salvo em {} e no histórico (custo estimado US$ {}).", file.toAbsolutePath().normalize(),
                cost.toPlainString());
    }

    /** Guarda as metas do bloco da IA, com o valor de partida calculado neste Raio-X. */
    private void saveGoals(CoachDiagnosis diagnosis, ProfileXRay xRay, FocusGoalBlock.Parsed parsed) {
        parsed.ignored().forEach(reason -> log.warn("Diagnóstico: meta ignorada: {}.", reason));
        if (parsed.goals().isEmpty()) {
            log.warn("Diagnóstico: o coach não mandou metas que o app consiga conferir; o próximo Raio-X compara"
                    + " só pelo texto.");
            return;
        }
        List<FocusGoal> goals = new ArrayList<>();
        for (int i = 0; i < parsed.goals().size(); i++) {
            FocusGoalBlock.Draft d = parsed.goals().get(i);
            goals.add(new FocusGoal(diagnosis, i + 1, d.title(), d.metric(), d.heroName(),
                    FocusGoalChecker.xRayValue(xRay, d.metric(), d.heroName()), d.target()));
        }
        history.saveGoals(goals);
        log.info("Diagnóstico: {} meta(s) guardada(s) para conferir no próximo Raio-X e nas partidas.", goals.size());
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
