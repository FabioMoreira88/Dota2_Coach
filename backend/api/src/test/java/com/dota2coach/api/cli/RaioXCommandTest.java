package com.dota2coach.api.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dota2coach.analysis.metric.XRayCalculator;
import com.dota2coach.analysis.report.XRayMarkdownRenderer;
import com.dota2coach.api.config.RaioXProperties;
import com.dota2coach.collector.service.ProfileSnapshotService;
import com.dota2coach.core.enums.Position;
import com.dota2coach.core.model.PlayerSummary;
import com.dota2coach.core.model.ProfileSnapshot;
import com.dota2coach.core.model.SteamId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;

class RaioXCommandTest {

    private static final long ACCOUNT = 34839344L;

    @TempDir
    Path dir;

    private final ProfileSnapshotService snapshotService = mock(ProfileSnapshotService.class);

    private RaioXCommand command(int parseLimit) {
        RaioXProperties props = new RaioXProperties(
                ACCOUNT, dir.resolve("fase0/raio-x-{accountId}.md").toString(), parseLimit, ZoneId.of("UTC"),
                Position.MID);
        return new RaioXCommand(snapshotService, new XRayCalculator(),
                new XRayMarkdownRenderer(props.zone()), props);
    }

    private static ProfileSnapshot emptySnapshot() {
        PlayerSummary player = new PlayerSummary(
                SteamId.fromAccountId(ACCOUNT), "Heisenberg", null, 53, 0, 0, List.of());
        return new ProfileSnapshot(player, List.of(), Map.of(), Map.of(), Map.of(), Map.of(),
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
    }

    @Test
    void limiteZeroNaoPedeAnalise() {
        when(snapshotService.collect(ACCOUNT)).thenReturn(emptySnapshot());

        command(0).run(new DefaultApplicationArguments());

        verify(snapshotService, never()).requestReplayParsing(anyList(), anyInt());
    }

    @Test
    void valoresPadraoDasPropriedades() {
        RaioXProperties props = new RaioXProperties(ACCOUNT, null, null, null, null);

        assertThat(props.resolvedOutput()).isEqualTo("../../docs/fase0/raio-x-34839344.md");
        assertThat(props.parseLimit()).isEqualTo(20);
        assertThat(props.zone()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
    }
}
