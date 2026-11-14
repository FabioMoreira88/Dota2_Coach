package com.dota2coach.api.cli;

import com.dota2coach.analysis.metric.MatchAnalysis;
import com.dota2coach.analysis.metric.MatchAnalysisCalculator;
import com.dota2coach.analysis.metric.ProfileXRay;
import com.dota2coach.analysis.report.MatchAnalysisMarkdownRenderer;
import com.dota2coach.api.config.CoachConfig.CoachClientFactory;
import com.dota2coach.api.config.CoachProperties;
import com.dota2coach.api.config.PartidaProperties;
import com.dota2coach.coachai.client.CoachReply;
import com.dota2coach.coachai.prompt.CoachPromptFactory;
import com.dota2coach.collector.service.MatchDataService;
import com.dota2coach.core.exception.ExternalServiceUnavailableException;
import com.dota2coach.core.model.CoachDiagnosis;
import com.dota2coach.core.model.MatchReview;
import com.dota2coach.core.model.MatchReviewComment;
import com.dota2coach.core.model.MatchSnapshot;
import com.dota2coach.core.model.XRayReport;
import com.dota2coach.core.service.MatchReviewService;
import com.dota2coach.core.service.XRayHistoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Analisa uma partida escolhida ao subir a aplicação com {@code --dota2coach.partida.match-id=...} e
 * {@code --dota2coach.partida.account-id=...}, comparando com o último Raio-X salvo da conta, e guarda a análise no
 * histórico. Com {@code --dota2coach.partida.comment=true}, o coach comenta a partida.
 * Com {@code --spring.main.web-application-type=none} a aplicação termina ao final.
 */
@Component
@ConditionalOnProperty(prefix = "dota2coach.partida", name = "match-id")
public class PartidaCommand implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PartidaCommand.class);

    private final MatchDataService matchDataService;
    private final MatchAnalysisCalculator calculator;
    private final MatchAnalysisMarkdownRenderer renderer;
    private final PartidaProperties properties;
    private final XRayHistoryService history;
    private final MatchReviewService reviews;
    private final ObjectMapper objectMapper;
    private final CoachProperties coachProperties;
    private final CoachClientFactory clientFactory;
    private final Clock clock;

    public PartidaCommand(
            MatchDataService matchDataService,
            MatchAnalysisCalculator calculator,
            MatchAnalysisMarkdownRenderer renderer,
            PartidaProperties properties,
            XRayHistoryService history,
            MatchReviewService reviews,
            ObjectMapper objectMapper,
            CoachProperties coachProperties,
            CoachClientFactory clientFactory,
            Clock clock) {
        this.matchDataService = matchDataService;
        this.calculator = calculator;
        this.renderer = renderer;
        this.properties = properties;
        this.history = history;
        this.reviews = reviews;
        this.objectMapper = objectMapper;
        this.coachProperties = coachProperties;
        this.clientFactory = clientFactory;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        long matchId = properties.matchId();
        if (properties.accountId() == null) {
            throw new IllegalStateException("Informe a conta com --dota2coach.partida.account-id=...");
        }
        if (properties.position() == null) {
            log.warn("Partida: posição principal não informada. Use --dota2coach.partida.position=mid (ou carry,"
                    + " offlane, soft-support, hard-support).");
        }
        log.info("Partida: buscando a partida {} na OpenDota...", matchId);
        MatchSnapshot snapshot = matchDataService.collect(matchId, properties.accountId())
                .orElseThrow(() -> new IllegalStateException(
                "A OpenDota ainda não tem o resultado da partida " + matchId + ". Confira o id."));
        if (!snapshot.match().replayParsed()) {
            requestParse(matchId);
        }

        XRayReport latestReport = history.latest(properties.accountId()).orElse(null);
        MatchAnalysis analysis = calculator.analyze(snapshot, properties.accountId(), properties.position(),
                xRayOf(latestReport));
        String markdown = renderer.render(analysis);
        Path output = write(Path.of(properties.resolvedOutput()), markdown);
        log.info("Partida: relatório salvo em {}", output);

        MatchReview review = reviews.saveReview(new MatchReview(properties.accountId(), matchId,
                properties.position(), clock.instant(), analysis.replayParsed(), toJson(analysis), markdown));
        log.info("Partida: guardada no histórico de análises da conta.");

        if (properties.comment()) {
            if (analysis.replayParsed()) {
                comment(review, latestReport, output);
            } else {
                log.warn("Partida: sem replay analisado o coach teria só o resumo; o comentário fica para quando"
                        + " você rodar de novo, com o replay pronto.");
            }
        }
    }

    private void comment(MatchReview review, XRayReport latestReport, Path reportFile) {
        String name = properties.coachProvider();
        CoachProperties.Provider provider = coachProperties.providers().get(name);
        if (provider == null) {
            throw new IllegalStateException("Provedor de IA '" + name + "' não está em dota2coach.coach.providers");
        }
        if (provider.apiKeyRequired() && !provider.hasApiKey()) {
            throw new IllegalStateException("Falta a chave do provedor '" + name + "' no .env");
        }
        String currentFocus = latestReport == null || latestReport.getId() == null ? null
                : history.diagnoses(latestReport.getId()).stream().findFirst().map(CoachDiagnosis::getText)
                        .orElse(null);
        log.info("Partida: o coach está comentando com {} (leva cerca de 1 minuto)...", name);
        CoachReply reply = clientFactory.apply(name, provider)
                .generate(CoachPromptFactory.fromMatchAnalysis(review.getMarkdown(), currentFocus));
        BigDecimal cost = BigDecimal.valueOf(provider.pricing().cost(reply.inputTokens(), reply.outputTokens()))
                .setScale(4, RoundingMode.HALF_UP);
        reviews.saveComment(new MatchReviewComment(review, name, reply.model(), clock.instant(),
                reply.inputTokens(), reply.outputTokens(), cost, reply.text()));
        Path file = reportFile.resolveSibling("comentario-" + reportFile.getFileName());
        Path written = write(file, reply.text() + "\n");
        log.info("Partida: comentário do coach salvo em {} e no histórico (custo estimado US$ {}).", written,
                cost.toPlainString());
    }

    private String toJson(MatchAnalysis analysis) {
        try {
            return objectMapper.writeValueAsString(analysis);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível converter a análise da partida em JSON", e);
        }
    }

    private void requestParse(long matchId) {
        try {
            matchDataService.requestReplayParsing(matchId);
            log.info("Partida: o replay ainda não foi analisado pela OpenDota. Análise pedida; rode de novo em uns"
                    + " 15 minutos para ver rotas, mortes e itens.");
        } catch (ExternalServiceUnavailableException e) {
            log.warn("Partida: o replay ainda não foi analisado e não foi possível pedir a análise: {}",
                    e.getMessage());
        }
    }

    /** Números do último Raio-X salvo da conta, para comparar a partida com o padrão do jogador. */
    private ProfileXRay xRayOf(XRayReport report) {
        if (report == null) {
            log.info("Partida: ainda não há Raio-X salvo desta conta; a partida não será comparada com o seu padrão.");
            return null;
        }
        try {
            return objectMapper.readValue(report.getMetricsJson(), ProfileXRay.class);
        } catch (JsonProcessingException e) {
            log.warn("Partida: não foi possível ler o último Raio-X salvo ({}); seguindo sem comparar com o seu"
                    + " padrão.", e.getOriginalMessage());
            return null;
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
