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
        assertThat(md).contains("- Wards colocadas: 1 observer e 2 sentry.", "| 10 | +1000 |", "| 20 | 0 |");
        assertThat(md).contains("## Contexto da partida", "### Draft", "| 1 | adversário | Axe | 2 (meio, maior patrimônio do time) |",
                "| 2 | seu | Ogre Magi | 1 (rota segura, menor patrimônio do time), pick fora do comum |",
                "Seu adversário direto pela posição: Axe.",
                "| 3 | seu | **Sniper (você)** | 2 (informada) |", "Você foi o 2º pick do seu time.",
                "| Heróis | Sniper, Ogre Magi | Axe |",
                "| Dano causado em heróis | físico 45,5%, mágico 54,5%, puro 0,0% | físico 33,3%, mágico 0,0%, puro 66,7% |",
                "Dano que você recebeu de heróis: físico 60,0%, mágico 0,0%, puro 40,0%.",
                "| Axe (adversário direto) | 47,0% | 1000 |",
                "| 30 a 40 min | 45,0% | – |", "| 40 a 50 min | 40,0% | 60,0% |");
        assertThat(md.indexOf("## Contexto da partida")).isLessThan(md.indexOf("## Fase de rotas"));
        assertThat(md).contains("## Itens dos dois times",
                "| seu | **Sniper (você)** | Power Treads 12:00, Maelstrom 18:00, Hurricane Pike 28:20 | "
                        + "Black King Bar, Desolator |",
                "| seu | Ogre Magi | nenhum | – |", "| adversário | Axe | nenhum | – |");
        assertThat(md).contains("## Tempos de item contra o normal do herói",
                "| **Sniper (você)** | Power Treads | 12:00 | 10 a 12 min | no normal | 55,0% |",
                "| **Sniper (você)** | Maelstrom | 18:00 | 12 a 15 min | depois do normal | 50,0% |");
        assertThat(md).contains("## De onde veio seu ouro e XP",
                "- Ouro: Creeps de rota 60,0% (6000), Heróis 20,0% (2000), Neutros 20,0% (2000).",
                "- XP: Creeps e neutros 80,0% (8000), Heróis 20,0% (2000).");
        assertThat(md).contains("## De quem veio o dano que você recebeu", "| Axe | 3000 | 75,0% |",
                "| Creeps, torres e Roshan | – | 25,0% |", "Ouro perdido ao morrer: 500. Ouro gasto em buyback: 300.");
        assertThat(md).contains("## Uso de itens e habilidades", "- Habilidades: Shrapnel 10x, sniper\\_assassinate 4x.",
                "- Itens ativados: tpscroll 3x, Black King Bar 2x.", "- Stun causado em heróis: 3,5 s.");
        assertThat(md).contains("## Visão e mapa", "- Wards adversárias destruídas: 1 observer e 0 sentry.",
                "- Runas pegas: Bounty 3, Dano dobrado 1.", "- Camps de neutros stackados: 2.");
        assertThat(md).contains("## Momentos para rever no replay",
                "- **4:30 a 5:05:** você morreu fora de luta para Axe.",
                "- **31:20 a 32:10:** luta com -1500 de ouro para seu time; você morreu.");
        assertThat(md.indexOf("## Momentos para rever no replay")).isGreaterThan(md.indexOf("## Lutas"));
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
                "## Objetivos", "## Lutas", "## Itens dos dois times",
                "## Tempos de item", "| Posição |");
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
