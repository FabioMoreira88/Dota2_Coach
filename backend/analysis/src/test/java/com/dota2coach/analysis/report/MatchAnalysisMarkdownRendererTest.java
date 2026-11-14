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
        assertThat(md).contains("## Contexto da partida", "### Draft", "| 1 | adversário | Axe |",
                "| 3 | seu | **Sniper (você)** |", "Você foi o 2º pick do seu time.",
                "| Heróis | Sniper, Ogre Magi | Axe |",
                "| Dano causado em heróis | físico 45,5%, mágico 54,5%, puro 0,0% | físico 33,3%, mágico 0,0%, puro 66,7% |",
                "Dano que você recebeu de heróis: físico 60,0%, mágico 0,0%, puro 40,0%.",
                "| Axe (sua rota) | 47,0% | 1000 |",
                "| 30 a 40 min | 45,0% | – |", "| 40 a 50 min | 40,0% | 60,0% |");
        assertThat(md.indexOf("## Contexto da partida")).isLessThan(md.indexOf("## Fase de rotas"));
        assertThat(md).contains("## Objetivos", "Torres derrubadas: seu time 1, adversário 1.",
                "| 5:00 | Adversário | tirou o first blood |", "| 11:40 | Seu time | derrubou a torre T1 do meio |",
                "| 38:20 | Adversário | derrubou o barracks à distância do meio |");
        assertThat(md).contains("## Lutas que mais mexeram no ouro",
                "A OpenDota detectou 3 lutas com 3 ou mais mortes. Seu time ganhou mais ouro que o adversário em 1, "
                        + "e você participou de 2.",
                "| 10:00 a 10:40 | +300 | Ogre Magi | Axe (2) | participou |",
                "| 31:20 a 32:10 | -1500 | Sniper, Ogre Magi (1 buyback) | nenhuma | participou e morreu |",
                "| 35:00 a 35:30 | -200 | Ogre Magi | Axe | não participou |");
    }

    @Test
    void semReplayAnalisadoAvisaEParaNoResumo() {
        String md = renderer.render(calculator.analyze(sniperMid(false), ACCOUNT, null, null));

        assertThat(md).contains("- **Posição principal (informada pelo jogador):** não informada",
                "| | Nesta partida |\n|---|---:|", "Ainda não há Raio-X salvo desta conta",
                "## Replay ainda não analisado");
        assertThat(md).doesNotContain("Rota nesta partida", "## Mortes", "## Fase de rotas", "Dano causado",
                "## Objetivos", "## Lutas");
        assertThat(md).as("draft, matchups e força por duração não dependem do replay")
                .contains("## Contexto da partida", "| Axe | 47,0% | 1000 |");
    }

    @Test
    void relogioDeJogo() {
        assertThat(MatchAnalysisMarkdownRenderer.clock(-80)).isEqualTo("-1:20");
        assertThat(MatchAnalysisMarkdownRenderer.clock(605)).isEqualTo("10:05");
        assertThat(MatchAnalysisMarkdownRenderer.minutes(125)).isEqualTo("2 min 05 s");
    }
}
