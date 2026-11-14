package com.dota2coach.analysis.report;

import static com.dota2coach.analysis.support.Snapshots.MAELSTROM;
import static com.dota2coach.analysis.support.Snapshots.SNIPER;
import static com.dota2coach.analysis.support.Snapshots.TREADS;
import static com.dota2coach.analysis.support.Snapshots.match;
import static com.dota2coach.analysis.support.Snapshots.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

import com.dota2coach.analysis.metric.XRayCalculator;
import com.dota2coach.core.enums.Lane;
import com.dota2coach.core.enums.Position;
import com.dota2coach.core.model.MatchRecord;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class XRayMarkdownRendererTest {

    private final XRayMarkdownRenderer renderer = new XRayMarkdownRenderer(ZoneId.of("America/Sao_Paulo"));

    @Test
    void geraTodasAsSecoesComNumerosEmPortugues() {
        List<MatchRecord> matches = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            matches.add(match(i + 1, SNIPER, i % 3 != 0, i, i < 5 ? Lane.MID_LANE : Lane.UNKNOWN,
                    List.of(TREADS, MAELSTROM)));
        }

        String md = renderer.render(new XRayCalculator().calculate(snapshot(53, matches), Position.MID));

        assertThat(md).startsWith("# Raio-X de Heisenberg\n");
        assertThat(md).contains("- **Gerado em:** 11/11/2026 09:00");
        assertThat(md).contains("- **Medalha:** Lenda 3");
        assertThat(md).contains("- **Posição principal (informada pelo jogador):** Meio (posição 2)");
        assertThat(md).doesNotContain("Funções dos heróis");
        assertThat(md).contains("| Taxa de vitória | 66,7% | 66,7% |");
        assertThat(md).contains("| Sniper | 30 | 66,7% | 4,0 | 500 | 600 | 30 (66,7%) | 54,0% |");
        assertThat(md).contains("| Meio | 5 |");
        assertThat(md).contains("## Fundamentos", "| Mortes por partida | 4,0 | 4,0 | 0,0 |");
        assertThat(md).contains("### Sniper", "- **Aderência:** 100,0%");
        assertThat(md).contains("- [ ] Total de partidas: 30, taxa de vitória 66,7%");
    }

    @Test
    void perfilVazioNaoQuebra() {
        String md = renderer.render(new XRayCalculator().calculate(snapshot(null, List.of())));

        assertThat(md).contains("- **Medalha:** sem medalha",
                "- **Posição principal (informada pelo jogador):** não informada", "_Sem partidas no recorte._",
                "em todas as faixas (você está sem medalha)");
    }

    @Test
    void medalhaEEscape() {
        assertThat(XRayMarkdownRenderer.medal(80)).isEqualTo("Imortal");
        assertThat(XRayMarkdownRenderer.medal(11)).isEqualTo("Arauto 1");
        assertThat(XRayMarkdownRenderer.escape("a|b_<c>")).isEqualTo("a\\|b\\_&lt;c&gt;");
    }
}
