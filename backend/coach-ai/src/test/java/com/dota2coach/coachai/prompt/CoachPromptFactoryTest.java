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
}
