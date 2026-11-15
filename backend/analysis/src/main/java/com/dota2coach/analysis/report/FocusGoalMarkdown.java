package com.dota2coach.analysis.report;

import static com.dota2coach.analysis.report.XRayMarkdownRenderer.dec;
import static com.dota2coach.analysis.report.XRayMarkdownRenderer.escape;
import static com.dota2coach.analysis.report.XRayMarkdownRenderer.pct;

import com.dota2coach.analysis.evolution.FocusGoalCheck;
import com.dota2coach.core.enums.FocusMetric;
import java.util.List;

/** Tabelas das metas dos focos, no Raio-X e na análise de partida. */
final class FocusGoalMarkdown {

    private static final String NONE = "—";

    private FocusGoalMarkdown() {}

    static void xRay(StringBuilder md, List<FocusGoalCheck> checks) {
        if (checks == null || checks.isEmpty()) {
            return;
        }
        md.append("## Metas do último diagnóstico\n\n");
        md.append("Calculado pelo app com os números deste Raio-X. \"No diagnóstico\" é o valor do Raio-X em que ")
                .append("a meta foi definida.\n\n");
        md.append("| Foco | Métrica | No diagnóstico | Meta | Agora | Resultado |\n|---|---|---:|---:|---:|---|\n");
        for (FocusGoalCheck c : checks) {
            md.append("| ").append(c.ordinal()).append(". ").append(escape(c.title()))
                    .append(" | ").append(metric(c))
                    .append(" | ").append(value(c.baseline(), c.metric()))
                    .append(" | ").append(target(c))
                    .append(" | ").append(value(c.current(), c.metric()))
                    .append(" | ").append(c.status().label()).append(" |\n");
        }
        md.append('\n');
    }

    static void match(StringBuilder md, List<FocusGoalCheck> checks) {
        if (checks == null || checks.isEmpty()) {
            return;
        }
        md.append("## Seus focos nesta partida\n\n");
        md.append("Metas do seu último diagnóstico, conferidas pelo app com os números desta partida. ")
                .append("Uma partida sozinha não fecha uma meta de média: veja como um sinal.\n\n");
        md.append("| Foco | Métrica | Meta | Nesta partida | Resultado |\n|---|---|---:|---:|---|\n");
        for (FocusGoalCheck c : checks) {
            md.append("| ").append(c.ordinal()).append(". ").append(escape(c.title()))
                    .append(" | ").append(metric(c))
                    .append(" | ").append(target(c))
                    .append(" | ").append(value(c.current(), c.metric()))
                    .append(" | ").append(c.status().label()).append(" |\n");
        }
        md.append('\n');
    }

    private static String metric(FocusGoalCheck c) {
        return c.metric().label() + (c.heroName() == null ? "" : " (" + escape(c.heroName()) + ")");
    }

    private static String target(FocusGoalCheck c) {
        return (c.metric().higherIsBetter() ? "≥ " : "≤ ") + value(c.target(), c.metric());
    }

    static String value(Double value, FocusMetric metric) {
        if (value == null) {
            return NONE;
        }
        return switch (metric.unit()) {
            case PERCENT -> pct(value);
            case SECONDS -> {
                long s = Math.round(value);
                yield s / 60 + ":" + String.format("%02d", s % 60);
            }
            case NUMBER -> dec(value);
        };
    }
}
