package com.dota2coach.api.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dota2coach.analysis.metric.XRayCalculator;
import com.dota2coach.analysis.report.XRayMarkdownRenderer;
import com.dota2coach.api.config.CoachConfig.CoachClientFactory;
import com.dota2coach.api.config.CoachProperties;
import com.dota2coach.api.config.RaioXProperties;
import com.dota2coach.coachai.client.CoachModelClient;
import com.dota2coach.coachai.client.CoachReply;
import com.dota2coach.coachai.enums.ProviderType;
import com.dota2coach.coachai.prompt.CoachPrompt;
import com.dota2coach.collector.service.ProfileSnapshotService;
import com.dota2coach.core.enums.Position;
import com.dota2coach.core.model.CoachDiagnosis;
import com.dota2coach.core.model.PlayerSummary;
import com.dota2coach.core.model.ProfileSnapshot;
import com.dota2coach.core.model.SteamId;
import com.dota2coach.core.model.XRayReport;
import com.dota2coach.core.service.XRayHistoryService;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;

class RaioXCommandTest {

    private static final long ACCOUNT = 34839344L;

    @TempDir
    Path dir;

    private final ProfileSnapshotService snapshotService = mock(ProfileSnapshotService.class);
    private final XRayHistoryService history = mock(XRayHistoryService.class);
    private final List<CoachPrompt> prompts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(history.saveReport(any())).thenAnswer(inv -> inv.getArgument(0));
        when(history.saveDiagnosis(any())).thenAnswer(inv -> inv.getArgument(0));
        when(history.count(ACCOUNT)).thenReturn(1L);
    }

    private RaioXCommand command(int parseLimit) {
        return command(parseLimit, false, "chave");
    }

    private RaioXCommand command(int parseLimit, boolean diagnose, String apiKey) {
        RaioXProperties props = new RaioXProperties(
                ACCOUNT, dir.resolve("fase0/raio-x-{accountId}.md").toString(), parseLimit, ZoneId.of("UTC"),
                Position.MID, diagnose, null);
        CoachProperties coach = new CoachProperties(null, null, null, null, Map.of("claude",
                new CoachProperties.Provider(ProviderType.ANTHROPIC, null, apiKey, "claude-opus-5-5", true, true,
                        "high", 4.0, 20.0, null)));
        CoachClientFactory factory = (name, provider) -> new CoachModelClient() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public CoachReply generate(CoachPrompt prompt) {
                prompts.add(prompt);
                return new CoachReply("## Foco 1", "claude-opus-5-5", 5000, 4000, Duration.ofSeconds(40));
            }
        };
        return new RaioXCommand(snapshotService, new XRayCalculator(), new XRayMarkdownRenderer(props.zone()), props,
                history, JsonMapper.builder().findAndAddModules().build(), coach, factory,
                Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneOffset.UTC));
    }

    private static ProfileSnapshot emptySnapshot() {
        PlayerSummary player = new PlayerSummary(
                SteamId.fromAccountId(ACCOUNT), "Heisenberg", null, 53, 0, 0, List.of());
        return new ProfileSnapshot(player, List.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                Instant.parse("2026-11-11T12:00:00Z"));
    }

    @Test
    void coletaPedeAnaliseESalvaORelatorio() throws Exception {
        ProfileSnapshot snapshot = emptySnapshot();
        when(snapshotService.collect(ACCOUNT)).thenReturn(snapshot);
        when(snapshotService.requestReplayParsing(snapshot.matches(), 5)).thenReturn(List.of(1L, 2L));

        command(5).run(new DefaultApplicationArguments());

        Path report = dir.resolve("fase0/raio-x-34839344.md");
        assertThat(report).exists();
        assertThat(Files.readString(report)).startsWith("# Raio-X de Heisenberg")
                .contains("Meio (posição 2)");
        verify(snapshotService).requestReplayParsing(snapshot.matches(), 5);

        ArgumentCaptor<XRayReport> saved = ArgumentCaptor.forClass(XRayReport.class);
        verify(history).saveReport(saved.capture());
        assertThat(saved.getValue().getAccountId()).isEqualTo(ACCOUNT);
        assertThat(saved.getValue().getPosition()).isEqualTo(Position.MID);
        assertThat(saved.getValue().getMarkdown()).isEqualTo(Files.readString(report));
        assertThat(saved.getValue().getMetricsJson()).contains("\"playerName\":\"Heisenberg\"", "\"mainPosition\":\"MID\"");
        verify(history, never()).saveDiagnosis(any());
        assertThat(prompts).isEmpty();
    }

    @Test
    void comDiagnosticoChamaAIASalvaNoHistoricoEEmArquivo() throws Exception {
        when(snapshotService.collect(ACCOUNT)).thenReturn(emptySnapshot());

        command(0, true, "chave").run(new DefaultApplicationArguments());

        assertThat(prompts).hasSize(1);
        assertThat(prompts.get(0).user()).contains("# Raio-X de Heisenberg");
        ArgumentCaptor<CoachDiagnosis> saved = ArgumentCaptor.forClass(CoachDiagnosis.class);
        verify(history).saveDiagnosis(saved.capture());
        assertThat(saved.getValue().getProvider()).isEqualTo("claude");
        assertThat(saved.getValue().getCostUsd()).isEqualByComparingTo("0.1000");
        assertThat(saved.getValue().getText()).isEqualTo("## Foco 1");
        assertThat(Files.readString(dir.resolve("fase0/diagnostico-34839344.md"))).isEqualTo("## Foco 1\n");
    }

    @Test
    void diagnosticoSemChaveFalhaDepoisDeSalvarORaioX() {
        when(snapshotService.collect(ACCOUNT)).thenReturn(emptySnapshot());

        assertThatThrownBy(() -> command(0, true, "").run(new DefaultApplicationArguments()))
                .hasMessageContaining("Falta a chave do provedor 'claude'");
        verify(history).saveReport(any());
        assertThat(prompts).isEmpty();
    }

    @Test
    void limiteZeroNaoPedeAnalise() {
        when(snapshotService.collect(ACCOUNT)).thenReturn(emptySnapshot());

        command(0).run(new DefaultApplicationArguments());

        verify(snapshotService, never()).requestReplayParsing(anyList(), anyInt());
    }

    @Test
    void valoresPadraoDasPropriedades() {
        RaioXProperties props = new RaioXProperties(ACCOUNT, null, null, null, null, null, null);

        assertThat(props.resolvedOutput()).isEqualTo("../../docs/fase0/raio-x-34839344.md");
        assertThat(props.parseLimit()).isEqualTo(20);
        assertThat(props.diagnose()).isFalse();
        assertThat(props.coachProvider()).isEqualTo("claude");
        assertThat(props.zone()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
    }
}
