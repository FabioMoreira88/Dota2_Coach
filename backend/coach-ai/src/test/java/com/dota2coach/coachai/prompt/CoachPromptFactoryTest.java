package com.dota2coach.coachai.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CoachPromptFactoryTest {

    @Test
    void colocaORaioXEntreTagsNaMensagem() {
        CoachPrompt prompt = CoachPromptFactory.fromXRayReport("# Raio-X de Heisenberg\n\n| a | b |\n");

        assertThat(prompt.user()).contains("<raio_x>\n# Raio-X de Heisenberg\n\n| a | b |\n</raio_x>");
        assertThat(prompt.system()).contains("no máximo 3 focos").contains("Não sugira picks")
                .contains("informada pelo próprio jogador", "não tente adivinhar");
    }

    @Test
    void recusaRelatorioVazio() {
        assertThatThrownBy(() -> CoachPromptFactory.fromXRayReport("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void partidaVaiEntreTagsComOsFocosAtuaisQuandoExistem() {
        CoachPrompt prompt = CoachPromptFactory.fromMatchAnalysis("# Análise da partida 1\n", "## Foco 1: last hits");

        assertThat(prompt.user()).contains("<partida>\n# Análise da partida 1\n</partida>",
                "<focos_atuais>\n## Foco 1: last hits\n</focos_atuais>");
        assertThat(prompt.system()).contains("você não viu o replay", "no máximo 3 lições", "Não sugira picks",
                "no máximo 650 palavras", "Dicas do matchup", "força dos times por duração");
    }

    @Test
    void partidaSemFocosNaoMandaATag() {
        CoachPrompt prompt = CoachPromptFactory.fromMatchAnalysis("# Análise da partida 1", null);

        assertThat(prompt.user()).doesNotContain("focos_atuais");
        assertThatThrownBy(() -> CoachPromptFactory.fromMatchAnalysis(" ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
