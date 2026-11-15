package com.dota2coach.api.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dota2coach.analysis.metric.MatchAnalysisCalculator;
import com.dota2coach.analysis.metric.ProfileXRay;
import com.dota2coach.analysis.metric.XRayCalculator;
import com.dota2coach.analysis.report.MatchAnalysisMarkdownRenderer;
import com.dota2coach.api.config.CoachConfig.CoachClientFactory;
import com.dota2coach.api.config.CoachProperties;
import com.dota2coach.api.config.PartidaProperties;
import com.dota2coach.coachai.client.CoachModelClient;
import com.dota2coach.coachai.client.CoachReply;
import com.dota2coach.coachai.enums.ProviderType;
import com.dota2coach.coachai.prompt.CoachPrompt;
import com.dota2coach.collector.service.MatchDataService;
import com.dota2coach.core.enums.GameMode;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.FocusMetric;
import com.dota2coach.core.enums.Position;
import com.dota2coach.core.enums.ScoreArea;
import com.dota2coach.core.enums.Side;
import com.dota2coach.core.model.AnalyzedMatch;
import com.dota2coach.core.model.CoachDiagnosis;
import com.dota2coach.core.model.FocusGoal;
import com.dota2coach.core.model.Hero;
import com.dota2coach.core.model.HeroMatchups;
import com.dota2coach.core.model.MatchPlayer;
import com.dota2coach.core.model.MatchRecord;
import com.dota2coach.core.model.MatchAreaScore;
import com.dota2coach.core.model.MatchReview;
import com.dota2coach.core.model.MatchReviewComment;
import com.dota2coach.core.model.MatchSnapshot;
import com.dota2coach.core.model.PlayerSummary;
import com.dota2coach.core.model.ProfileSnapshot;
import com.dota2coach.core.model.SteamId;
import com.dota2coach.core.model.XRayReport;
import com.dota2coach.core.service.MatchReviewService;
import com.dota2coach.core.service.XRayHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

    private final MatchReviewService reviews = mock(MatchReviewService.class);
    private String replyText = "## Lição 1";
    private final List<CoachPrompt> prompts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(reviews.saveReview(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reviews.saveComment(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private PartidaCommand command(Long accountId) {
        return command(accountId, false, "chave");
    }

    private PartidaCommand command(Long accountId, boolean comment, String apiKey) {
        PartidaProperties props = new PartidaProperties(MATCH, accountId, Position.MID,
                dir.resolve("fase0/partida-{matchId}.md").toString(), ZoneId.of("UTC"), comment, null);
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
                return new CoachReply(replyText, "claude-opus-5-5", 4000, 2000, Duration.ofSeconds(30));
            }
        };
        return new PartidaCommand(matchDataService, new MatchAnalysisCalculator(),
                new MatchAnalysisMarkdownRenderer(props.zone()), props, history, reviews, objectMapper, coach,
                factory, Clock.fixed(Instant.parse("2026-11-14T20:00:00Z"), ZoneOffset.UTC));
    }

    private static MatchSnapshot snapshot(boolean parsed) {
        MatchPlayer me = new MatchPlayer(0, ACCOUNT, SNIPER, Side.RADIANT, parsed ? Lane.MID_LANE : Lane.UNKNOWN,
                parsed ? 2 : null, 8, 3, 6, 300, 15, 560, 650, 25000, 4000, 0, 21000, 25,
                parsed ? List.of(0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40) : List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), 0, 0, null, Map.of(), null, Map.of(), Map.of());
        AnalyzedMatch match = new AnalyzedMatch(MATCH, NOW, Duration.ofMinutes(40), true, GameMode.ALL_PICK, true,
                parsed, List.of(), List.of(), List.of(me), List.of(), List.of());
        return new MatchSnapshot(match, Map.of(SNIPER, new Hero(SNIPER, "Sniper", "agi", List.of())), Map.of(),
                Map.of(), Map.of(), HeroMatchups.empty(SNIPER), Map.of(), Map.of(),
                Map.of());
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
        when(matchDataService.collect(MATCH, ACCOUNT)).thenReturn(Optional.of(snapshot(true)));
        when(history.latest(ACCOUNT)).thenReturn(Optional.of(savedXRay()));

        command(ACCOUNT).run(new DefaultApplicationArguments());

        String md = Files.readString(dir.resolve("fase0/partida-9083198111.md"));
        assertThat(md).startsWith("# Análise da partida 9083198111\n")
                .contains("Seu normal com o herói (3 partidas recentes)", "| Ouro/min | 560 | 500 |",
                        "| **Sniper (você)** | 40 |");
        verify(matchDataService, never()).requestReplayParsing(MATCH);

        ArgumentCaptor<MatchReview> saved = ArgumentCaptor.forClass(MatchReview.class);
        verify(reviews).saveReview(saved.capture());
        assertThat(saved.getValue().getMatchId()).isEqualTo(MATCH);
        assertThat(saved.getValue().getAccountId()).isEqualTo(ACCOUNT);
        assertThat(saved.getValue().isReplayParsed()).isTrue();
        assertThat(saved.getValue().getMarkdown()).isEqualTo(md);
        assertThat(saved.getValue().getMetricsJson()).contains("\"heroName\":\"Sniper\"");
        verify(reviews, never()).saveComment(any());
        assertThat(prompts).isEmpty();
    }

    @Test
    void comComentarioOCoachRecebeAPartidaEOsFocosAtuais() throws Exception {
        XRayReport report = savedXRay();
        org.springframework.test.util.ReflectionTestUtils.setField(report, "id", 7L);
        when(matchDataService.collect(MATCH, ACCOUNT)).thenReturn(Optional.of(snapshot(true)));
        when(history.latest(ACCOUNT)).thenReturn(Optional.of(report));
        CoachDiagnosis diagnosis = new CoachDiagnosis(report, "claude", "claude-opus-5-5", NOW,
                1, 1, BigDecimal.ONE, "## Foco 1: last hits aos 10 min");
        org.springframework.test.util.ReflectionTestUtils.setField(diagnosis, "id", 9L);
        when(history.diagnoses(7L)).thenReturn(List.of(diagnosis));
        when(history.goals(9L)).thenReturn(List.of(new FocusGoal(diagnosis, 1, "Morrer menos",
                FocusMetric.DEATHS_PER_MATCH, null, 8.0, 5)));

        command(ACCOUNT, true, "chave").run(new DefaultApplicationArguments());

        assertThat(prompts).singleElement().satisfies(p -> assertThat(p.user())
                .contains("<partida>\n# Análise da partida 9083198111", "## Foco 1: last hits aos 10 min",
                        "## Seus focos nesta partida", "| 1. Morrer menos | Mortes por partida | ≤ 5,0 |"));
        ArgumentCaptor<MatchReviewComment> saved = ArgumentCaptor.forClass(MatchReviewComment.class);
        verify(reviews).saveComment(saved.capture());
        assertThat(saved.getValue().getCostUsd()).isEqualByComparingTo("0.0560");
        assertThat(Files.readString(dir.resolve("fase0/comentario-partida-9083198111.md"))).isEqualTo("## Lição 1\n");
    }

    @Test
    @SuppressWarnings("unchecked")
    void notasPorAreaSaoGuardadasEATendenciaVaiParaOCoach() throws Exception {
        when(matchDataService.collect(MATCH, ACCOUNT)).thenReturn(Optional.of(snapshot(true)));
        when(history.latest(ACCOUNT)).thenReturn(Optional.empty());
        MatchReviewComment old = mock(MatchReviewComment.class);
        when(reviews.scores(ACCOUNT)).thenReturn(List.of(
                new MatchAreaScore(old, ACCOUNT, 1L, NOW.minusSeconds(3600), ScoreArea.VISION, 4, null),
                new MatchAreaScore(old, ACCOUNT, MATCH, NOW.minusSeconds(60), ScoreArea.VISION, 9, "mesma partida")));
        replyText = "## Notas por área\n\n| Área | Nota | Justificativa |\n|---|---:|---|\n"
                + "| Rota | 7 | +319 de ouro |\n| Visão | 6 | 15 runas |\n";

        command(ACCOUNT, true, "chave").run(new DefaultApplicationArguments());

        assertThat(prompts).singleElement().satisfies(p -> assertThat(p.user())
                .contains("## Suas notas por área nas partidas anteriores", "| Visão | 1 | 4,0 | — | 4 |"));
        ArgumentCaptor<List<MatchAreaScore>> saved = ArgumentCaptor.forClass(List.class);
        verify(reviews).saveScores(saved.capture());
        assertThat(saved.getValue()).extracting(MatchAreaScore::getArea, MatchAreaScore::getScore,
                MatchAreaScore::getMatchId).containsExactly(tuple(ScoreArea.LANE, 7, MATCH),
                tuple(ScoreArea.VISION, 6, MATCH));
    }

    @Test
    void semReplayAnalisadoNaoGastaComOCoach() {
        when(matchDataService.collect(MATCH, ACCOUNT)).thenReturn(Optional.of(snapshot(false)));
        when(history.latest(ACCOUNT)).thenReturn(Optional.empty());

        command(ACCOUNT, true, "chave").run(new DefaultApplicationArguments());

        verify(reviews).saveReview(any());
        assertThat(prompts).isEmpty();
    }

    @Test
    void comentarioSemChaveFalhaDepoisDeSalvarAAnalise() {
        when(matchDataService.collect(MATCH, ACCOUNT)).thenReturn(Optional.of(snapshot(true)));
        when(history.latest(ACCOUNT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> command(ACCOUNT, true, "").run(new DefaultApplicationArguments()))
                .hasMessageContaining("Falta a chave do provedor 'claude'");
        verify(reviews).saveReview(any());
    }

    @Test
    void replayNaoAnalisadoPedeAAnaliseESalvaOResumo() throws Exception {
        when(matchDataService.collect(MATCH, ACCOUNT)).thenReturn(Optional.of(snapshot(false)));
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

        when(matchDataService.collect(MATCH, ACCOUNT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> command(ACCOUNT).run(new DefaultApplicationArguments()))
                .hasMessageContaining("Confira o id");
    }
}
