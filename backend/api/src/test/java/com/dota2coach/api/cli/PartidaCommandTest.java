package com.dota2coach.api.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dota2coach.analysis.metric.MatchAnalysisCalculator;
import com.dota2coach.analysis.metric.ProfileXRay;
import com.dota2coach.analysis.metric.XRayCalculator;
import com.dota2coach.analysis.report.MatchAnalysisMarkdownRenderer;
import com.dota2coach.api.config.PartidaProperties;
import com.dota2coach.collector.service.MatchDataService;
import com.dota2coach.core.enums.GameMode;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Position;
import com.dota2coach.core.enums.Side;
import com.dota2coach.core.model.AnalyzedMatch;
import com.dota2coach.core.model.Hero;
import com.dota2coach.core.model.MatchPlayer;
import com.dota2coach.core.model.MatchRecord;
import com.dota2coach.core.model.MatchSnapshot;
import com.dota2coach.core.model.PlayerSummary;
import com.dota2coach.core.model.ProfileSnapshot;
import com.dota2coach.core.model.SteamId;
import com.dota2coach.core.model.XRayReport;
import com.dota2coach.core.service.XRayHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;

class PartidaCommandTest {

    private static final long ACCOUNT = 34839344L;
    private static final long MATCH = 9083198111L;
    private static final int SNIPER = 35;
    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    @TempDir
    Path dir;

    private final MatchDataService matchDataService = mock(MatchDataService.class);
    private final XRayHistoryService history = mock(XRayHistoryService.class);
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    private PartidaCommand command(Long accountId) {
        PartidaProperties props = new PartidaProperties(MATCH, accountId, Position.MID,
                dir.resolve("fase0/partida-{matchId}.md").toString(), ZoneId.of("UTC"));
        return new PartidaCommand(matchDataService, new MatchAnalysisCalculator(),
                new MatchAnalysisMarkdownRenderer(props.zone()), props, history, objectMapper);
    }

    private static MatchSnapshot snapshot(boolean parsed) {
        MatchPlayer me = new MatchPlayer(0, ACCOUNT, SNIPER, Side.RADIANT, parsed ? Lane.MID_LANE : Lane.UNKNOWN,
                parsed ? 2 : null, 8, 3, 6, 300, 15, 560, 650, 25000, 4000, 0, 21000, 25,
                parsed ? List.of(0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40) : List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), 0, 0, null, Map.of(), null);
        AnalyzedMatch match = new AnalyzedMatch(MATCH, NOW, Duration.ofMinutes(40), true, GameMode.ALL_PICK, true,
                parsed, List.of(), List.of(me));
        return new MatchSnapshot(match, Map.of(SNIPER, new Hero(SNIPER, "Sniper", "agi", List.of())), Map.of());
    }

    /** Raio-X com 3 partidas de Sniper, salvo como JSON no histórico como o comando do Raio-X faz. */
    private XRayReport savedXRay() throws Exception {
        List<MatchRecord> matches = java.util.stream.IntStream.range(0, 3)
                .mapToObj(i -> new MatchRecord(i + 1, NOW.minusSeconds(86400L * i), Duration.ofMinutes(40), SNIPER,
                        Side.RADIANT, true, GameMode.ALL_PICK, true, 1, false, 6, 4, 10, 500, 600, 200, 5, 24000,
                        2000, 0, Lane.MID_LANE, List.of(), 0, true))
                .toList();
        PlayerSummary player = new PlayerSummary(SteamId.fromAccountId(ACCOUNT), "Heisenberg", null, 53, 0, 0,
                List.of());
        ProfileXRay xRay = new XRayCalculator().calculate(new ProfileSnapshot(player, matches,
                Map.of(SNIPER, new Hero(SNIPER, "Sniper", "agi", List.of())), Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of(), NOW), Position.MID);
        return new XRayReport(ACCOUNT, Position.MID, NOW, 3, objectMapper.writeValueAsString(xRay), "# Raio-X");
    }

    @Test
    void geraORelatorioComparandoComOUltimoRaioXSalvo() throws Exception {
        when(matchDataService.collect(MATCH)).thenReturn(Optional.of(snapshot(true)));
        when(history.latest(ACCOUNT)).thenReturn(Optional.of(savedXRay()));

        command(ACCOUNT).run(new DefaultApplicationArguments());

        String md = Files.readString(dir.resolve("fase0/partida-9083198111.md"));
        assertThat(md).startsWith("# Análise da partida 9083198111\n")
                .contains("Seu normal com o herói (3 partidas recentes)", "| Ouro/min | 560 | 500 |",
                        "| **Sniper (você)** | 40 |");
        verify(matchDataService, never()).requestReplayParsing(MATCH);
    }

    @Test
    void replayNaoAnalisadoPedeAAnaliseESalvaOResumo() throws Exception {
        when(matchDataService.collect(MATCH)).thenReturn(Optional.of(snapshot(false)));
        when(history.latest(ACCOUNT)).thenReturn(Optional.empty());

        command(ACCOUNT).run(new DefaultApplicationArguments());

        verify(matchDataService).requestReplayParsing(MATCH);
        assertThat(Files.readString(dir.resolve("fase0/partida-9083198111.md")))
                .contains("## Replay ainda não analisado", "Ainda não há Raio-X salvo desta conta");
    }

    @Test
    void semContaOuComPartidaDesconhecidaFalhaComMensagemClara() {
        assertThatThrownBy(() -> command(null).run(new DefaultApplicationArguments()))
                .hasMessageContaining("--dota2coach.partida.account-id");

        when(matchDataService.collect(MATCH)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> command(ACCOUNT).run(new DefaultApplicationArguments()))
                .hasMessageContaining("Confira o id");
    }
}
