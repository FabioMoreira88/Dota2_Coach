package com.dota2coach.analysis.report;

import static com.dota2coach.analysis.support.Matches.ACCOUNT;
import static com.dota2coach.analysis.support.Matches.sniperMid;
import static org.assertj.core.api.Assertions.assertThat;

import com.dota2coach.analysis.metric.MatchAnalysisCalculator;
import com.dota2coach.analysis.metric.MatchAnalysisCalculatorTestSupport;
import com.dota2coach.core.enums.Position;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class MatchAnalysisMarkdownRendererTest {

    private final MatchAnalysisMarkdownRenderer renderer =
            new MatchAnalysisMarkdownRenderer(ZoneId.of("America/Sao_Paulo"));
    private final MatchAnalysisCalculator calculator = new MatchAnalysisCalculator();

    @Test
    void geraAsSecoesDaPartidaComOPadraoDoJogador() {
        String md = renderer.render(calculator.analyze(sniperMid(true), ACCOUNT, Position.MID,
                MatchAnalysisCalculatorTestSupport.latestXRay()));

        assertThat(md).startsWith("# Análise da partida 9083198111\n");
        assertThat(md).contains("- **Herói:** Sniper", "- **Resultado:** derrota em 40 min 00 s",
                "- **Data:** 11/11/2026 09:00 (All Pick, ranqueada)",
                "- **Posição principal (informada pelo jogador):** Meio (posição 2)",
                "- **Rota nesta partida:** Meio",
                "https://www.opendota.com/matches/9083198111");
        assertThat(md).contains("| | Nesta partida | Seu normal com o herói (3 partidas recentes) |",
                "| Abates / mortes / assistências | 8 / 3 / 6 | – |", "| KDA | 4,7 | 4,0 |",
                "| Ouro/min | 560 | 500 |", "| Participação nos abates do time | 70,0% | – |");
        assertThat(md).contains("## Comparação com outros jogadores do herói", "| Ouro/min | p66 |", "Em mortes, percentil alto é ruim",
                "| Last hits aos 10 min | p40 |");
        assertThat(md).contains("## Fase de rotas (10 minutos)", "| **Sniper (você)** | 40 | 10 | 3000 | 3200 |",
                "| Axe (adversário) | 50 | 0 | 3500 | 3400 |", "Eficiência na fase de rotas (OpenDota): 72%.",
                "Seu normal: 41,0 last hits aos 10 minutos com o herói e 2,0 denies (média geral).");
        assertThat(md).contains("## Mortes", "| 5:00 | 0 a 10 min | Axe | 15 s |",
                "| 31:40 | 30 min ou mais | não identificado | 45 s |",
                "Tempo morto no total: 1 min 00 s (seu normal: 0 min 30 s por partida).",
                "| 0 a 10 min | 1 | 1,0 |");
        assertThat(md).contains("## Itens principais", "| Power Treads | 12:00 | 10,0 min |", "| Maelstrom | 18:00 | – |");
        assertThat(md).doesNotContain("Tango");
        assertThat(md).contains("Wards colocadas: 1 observer e 2 sentry.", "| 10 | +1000 |", "| 20 | 0 |");
    }

    @Test
    void semReplayAnalisadoAvisaEParaNoResumo() {
        String md = renderer.render(calculator.analyze(sniperMid(false), ACCOUNT, null, null));

        assertThat(md).contains("- **Posição principal (informada pelo jogador):** não informada",
                "| | Nesta partida |\n|---|---:|", "Ainda não há Raio-X salvo desta conta",
                "## Replay ainda não analisado");
        assertThat(md).doesNotContain("Rota nesta partida", "## Mortes", "## Fase de rotas");
    }

    @Test
    void relogioDeJogo() {
        assertThat(MatchAnalysisMarkdownRenderer.clock(-80)).isEqualTo("-1:20");
        assertThat(MatchAnalysisMarkdownRenderer.clock(605)).isEqualTo("10:05");
        assertThat(MatchAnalysisMarkdownRenderer.minutes(125)).isEqualTo("2 min 05 s");
    }
}
