package com.dota2coach.coachai.service;

import com.dota2coach.coachai.service.CoachComparisonService.Entry;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Gera dois arquivos Markdown: os textos às cegas, com uma tabela de notas, e o gabarito com modelo, tempo,
 * tokens e custo de cada letra.
 */
public class ComparisonReportRenderer {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ZoneId zone;

    public ComparisonReportRenderer(ZoneId zone) {
        this.zone = zone;
    }

    public String blind(List<Entry> entries, String reportName, Instant generatedAt) {
        StringBuilder md = new StringBuilder();
        md.append("# Comparativo de IA às cegas\n\n");
        md.append("Os textos abaixo foram escritos por modelos diferentes a partir do mesmo Raio-X (`")
                .append(reportName).append("`), em ").append(DATE_TIME.format(generatedAt.atZone(zone)))
                .append(". Dê as notas antes de abrir o gabarito.\n\n");
        for (Entry e : entries) {
            md.append("---\n\n## Texto ").append(e.label()).append("\n\n");
            if (e.succeeded()) {
                md.append(e.reply().text()).append("\n\n");
            } else {
                md.append("_Este modelo falhou e não gerou texto._\n\n");
            }
        }
        md.append("---\n\n## Suas notas (0 a 10)\n\n");
        md.append("| Texto | Os números citados estão certos? | Os focos fazem sentido para você? | ")
                .append("Dá para pôr em prática? | Nota geral |\n|---|---|---|---|---|\n");
        for (Entry e : entries) {
            md.append("| ").append(e.label()).append(" | | | | |\n");
        }
        return md.toString();
    }

    public String key(List<Entry> entries) {
        StringBuilder md = new StringBuilder();
        md.append("# Gabarito do comparativo de IA\n\n");
        md.append("| Texto | Provedor | Modelo | Tempo | Tokens (entrada / saída) | Custo estimado |\n")
                .append("|---|---|---|---:|---:|---:|\n");
        for (Entry e : entries) {
            md.append("| ").append(e.label()).append(" | ").append(e.provider()).append(" | ");
            if (e.succeeded()) {
                md.append(e.reply().model())
                        .append(" | ").append(String.format(Locale.ROOT, "%.1f s", e.reply().elapsed().toMillis() / 1000.0))
                        .append(" | ").append(e.reply().inputTokens()).append(" / ").append(e.reply().outputTokens())
                        .append(" | ").append(String.format(Locale.ROOT, "US$ %.4f", e.costUsd()));
            } else {
                md.append("falhou: ").append(e.error().replace("|", "\\|").replace("\n", " ")).append(" | — | — | —");
            }
            md.append(" |\n");
        }
        return md.toString();
    }
}
