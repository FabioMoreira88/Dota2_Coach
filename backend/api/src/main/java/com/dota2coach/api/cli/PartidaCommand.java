package com.dota2coach.api.cli;

import com.dota2coach.analysis.metric.MatchAnalysis;
import com.dota2coach.analysis.metric.MatchAnalysisCalculator;
import com.dota2coach.analysis.metric.ProfileXRay;
import com.dota2coach.analysis.report.MatchAnalysisMarkdownRenderer;
import com.dota2coach.api.config.PartidaProperties;
import com.dota2coach.collector.service.MatchDataService;
import com.dota2coach.core.exception.ExternalServiceUnavailableException;
import com.dota2coach.core.model.MatchSnapshot;
import com.dota2coach.core.model.XRayReport;
import com.dota2coach.core.service.XRayHistoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Analisa uma partida escolhida ao subir a aplicação com {@code --dota2coach.partida.match-id=...} e
 * {@code --dota2coach.partida.account-id=...}, comparando com o último Raio-X salvo da conta.
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
    private final ObjectMapper objectMapper;

    public PartidaCommand(
            MatchDataService matchDataService,
            MatchAnalysisCalculator calculator,
            MatchAnalysisMarkdownRenderer renderer,
            PartidaProperties properties,
            XRayHistoryService history,
            ObjectMapper objectMapper) {
        this.matchDataService = matchDataService;
        this.calculator = calculator;
        this.renderer = renderer;
        this.properties = properties;
        this.history = history;
        this.objectMapper = objectMapper;
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
        MatchSnapshot snapshot = matchDataService.collect(matchId).orElseThrow(() -> new IllegalStateException(
                "A OpenDota ainda não tem o resultado da partida " + matchId + ". Confira o id."));
        if (!snapshot.match().replayParsed()) {
            requestParse(matchId);
        }

        ProfileXRay latest = latestXRay(properties.accountId());
        MatchAnalysis analysis = calculator.analyze(snapshot, properties.accountId(), properties.position(), latest);
        Path output = write(Path.of(properties.resolvedOutput()), renderer.render(analysis));
        log.info("Partida: relatório salvo em {}", output);
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

    /** Último Raio-X salvo da conta, para comparar a partida com o padrão do jogador. */
    private ProfileXRay latestXRay(long accountId) {
        XRayReport report = history.latest(accountId).orElse(null);
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
