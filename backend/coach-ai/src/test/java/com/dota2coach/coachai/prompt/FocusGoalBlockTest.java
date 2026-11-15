package com.dota2coach.coachai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.dota2coach.core.enums.FocusMetric;
import org.junit.jupiter.api.Test;

class FocusGoalBlockTest {

    @Test
    void tiraOBlocoDoTextoEGuardaAsMetasValidas() {
        String reply = """
                ## Foco 1: last hits
                Meta: 45 aos 10 min.

                ## Ponto forte
                Ogre.

                ```metas
                {"focos": [
                  {"titulo": "Last hits na rota", "metrica": "last_hits_aos_10", "heroi": "Sniper", "meta": 45},
                  {"titulo": "Mortes no meio do jogo", "metrica": "mortes_10_20", "heroi": null, "meta": 1.8},
                  {"titulo": "Comunicação", "metrica": "comunicacao", "meta": 3},
                  {"titulo": "Sem número", "metrica": "kda", "meta": "alto"}
                ]}
                ```
                """;

        FocusGoalBlock.Parsed parsed = FocusGoalBlock.parse(reply);

        assertThat(parsed.text()).isEqualTo("## Foco 1: last hits\nMeta: 45 aos 10 min.\n\n## Ponto forte\nOgre.");
        assertThat(parsed.goals()).containsExactly(
                new FocusGoalBlock.Draft("Last hits na rota", FocusMetric.LAST_HITS_AT_10, "Sniper", 45),
                new FocusGoalBlock.Draft("Mortes no meio do jogo", FocusMetric.DEATHS_10_20, null, 1.8));
        assertThat(parsed.ignored()).containsExactly("métrica desconhecida 'comunicacao'", "meta sem número em 'kda'");
    }

    @Test
    void semBlocoOuComJsonQuebradoOTextoFicaInteiro() {
        assertThat(FocusGoalBlock.parse("## Foco 1\n").goals()).isEmpty();
        assertThat(FocusGoalBlock.parse("## Foco 1\n").text()).isEqualTo("## Foco 1");

        FocusGoalBlock.Parsed broken = FocusGoalBlock.parse("## Foco 1\n```metas\n{quebrado\n```\n");
        assertThat(broken.text()).isEqualTo("## Foco 1");
        assertThat(broken.goals()).isEmpty();
        assertThat(broken.ignored()).containsExactly("bloco de metas não é um JSON válido");
    }

    @Test
    void promptListaAsChavesDasMetricas() {
        assertThat(CoachPromptFactory.SYSTEM).contains("bloco de código marcado como metas",
                "  - last_hits_aos_10: Last hits aos 10 min (maior é melhor)",
                "  - mortes_10_20: Mortes de 10 a 20 min (menor é melhor)",
                "  - taxa_vitoria: Taxa de vitória (maior é melhor), de 0 a 1", "Metas do último diagnóstico");
    }
}
