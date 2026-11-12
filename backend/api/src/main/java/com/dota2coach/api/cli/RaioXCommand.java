package com.dota2coach.api.cli;

import com.dota2coach.analysis.metric.ProfileXRay;
import com.dota2coach.analysis.metric.XRayCalculator;
import com.dota2coach.analysis.report.XRayMarkdownRenderer;
import com.dota2coach.api.config.RaioXProperties;
import com.dota2coach.collector.service.ProfileSnapshotService;
import com.dota2coach.core.model.ProfileSnapshot;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Gera o relatório Raio-X em Markdown ao subir a aplicação com {@code --dota2coach.raio-x.account-id=...}.
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

    public RaioXCommand(
            ProfileSnapshotService snapshotService,
            XRayCalculator calculator,
            XRayMarkdownRenderer renderer,
            RaioXProperties properties) {
        this.snapshotService = snapshotService;
        this.calculator = calculator;
        this.renderer = renderer;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        long accountId = properties.accountId();
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

        ProfileXRay xRay = calculator.calculate(snapshot);
        Path output = write(renderer.render(xRay));
        log.info("Raio-X: relatório salvo em {}", output);
    }

    private Path write(String markdown) {
        Path output = Path.of(properties.resolvedOutput()).toAbsolutePath().normalize();
        try {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(output, markdown, StandardCharsets.UTF_8);
            return output;
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar o relatório em " + output, e);
        }
    }
}
