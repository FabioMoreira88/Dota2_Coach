package com.dota2coach.analysis.report;

import static com.dota2coach.analysis.report.XRayMarkdownRenderer.dec;

import com.dota2coach.analysis.evolution.AreaTrend;
import com.dota2coach.analysis.evolution.AreaTrendCalculator;
import java.util.List;
import java.util.stream.Collectors;

/** Tabela da tendência das notas por área, no Raio-X e na análise de partida. */
final class AreaTrendMarkdown {

    private AreaTrendMarkdown() {}

    static void render(StringBuilder md, String title, String intro, List<AreaTrend> trends) {
        if (trends == null || trends.isEmpty()) {
            return;
        }
        int window = AreaTrendCalculator.WINDOW;
        md.append("## ").append(title).append("\n\n").append(intro).append("\n\n");
        md.append("| Área | Partidas | Média das últimas ").append(window).append(" | Média das ").append(window)
                .append(" anteriores | Últimas notas (mais recente primeiro) |\n|---|---:|---:|---:|---|\n");
        for (AreaTrend t : trends) {
            md.append("| ").append(t.area().label())
                    .append(" | ").append(t.matches())
                    .append(" | ").append(dec(t.recentAverage()))
                    .append(" | ").append(t.previousAverage() == null ? "—" : dec(t.previousAverage()))
                    .append(" | ").append(t.lastScores().stream().map(String::valueOf)
                            .collect(Collectors.joining(", ")))
                    .append(" |\n");
        }
        md.append('\n');
    }
}
