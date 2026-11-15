package com.dota2coach.coachai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.dota2coach.core.enums.ScoreArea;
import org.junit.jupiter.api.Test;

class AreaScoreTableTest {

    @Test
    void leANotaDeCadaAreaDaSecao() {
        String comment = """
                ## Lições
                | Rota | 2 | não é a tabela de notas |

                ## Notas por área

                | Área | Nota | Justificativa |
                |---|---:|---|
                | Rota | 7 | +319 de ouro e +221 de XP sobre o Sniper aos 10 min |
                | **Lutas** | 6/10 | Dano em heróis no p92 |
                | Itens | 6,5 | Blink 11 min adiantado |
                | Visao | 6 | 15 runas |
                | Sobrevivência | 5 | 7 mortes |
                | Moral | 9 | não é área |

                ## Seus focos atuais
                | Rota | 1 | fora da seção |
                """;

        assertThat(AreaScoreTable.parse(comment)).containsExactly(
                new AreaScoreTable.Row(ScoreArea.LANE, 7, "+319 de ouro e +221 de XP sobre o Sniper aos 10 min"),
                new AreaScoreTable.Row(ScoreArea.FIGHTS, 6, "Dano em heróis no p92"),
                new AreaScoreTable.Row(ScoreArea.ITEMS, 6, "Blink 11 min adiantado"),
                new AreaScoreTable.Row(ScoreArea.VISION, 6, "15 runas"),
                new AreaScoreTable.Row(ScoreArea.SURVIVAL, 5, "7 mortes"));
    }

    @Test
    void semSecaoNaoTemNotas() {
        assertThat(AreaScoreTable.parse("## Resumo\n| Rota | 7 | x |")).isEmpty();
        assertThat(AreaScoreTable.parse(null)).isEmpty();
    }
}
