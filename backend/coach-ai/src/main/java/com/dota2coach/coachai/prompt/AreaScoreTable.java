package com.dota2coach.coachai.prompt;

import com.dota2coach.core.enums.ScoreArea;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lê a tabela "Notas por área" do comentário de partida. */
public final class AreaScoreTable {

    private static final Pattern HEADING = Pattern.compile("(?im)^#{1,6}\\s*Notas por [áa]rea\\s*$");
    private static final Pattern NEXT_HEADING = Pattern.compile("(?m)^#{1,6}\\s");
    private static final Pattern ROW = Pattern.compile(
            "^\\|\\s*([^|]+?)\\s*\\|\\s*\\**(\\d{1,2})(?:[,.]\\d+)?\\**\\s*(?:/\\s*10)?\\s*\\|\\s*(.*?)\\s*\\|?\\s*$");

    private AreaScoreTable() {}

    public record Row(ScoreArea area, int score, String reason) {}

    /** Notas de 0 a 10 das áreas conhecidas, uma por área, na ordem da tabela; vazio sem a seção. */
    public static List<Row> parse(String comment) {
        if (comment == null) {
            return List.of();
        }
        Matcher heading = HEADING.matcher(comment);
        if (!heading.find()) {
            return List.of();
        }
        String section = comment.substring(heading.end());
        Matcher next = NEXT_HEADING.matcher(section);
        if (next.find()) {
            section = section.substring(0, next.start());
        }
        List<Row> rows = new ArrayList<>();
        Set<ScoreArea> seen = EnumSet.noneOf(ScoreArea.class);
        for (String line : section.split("\\R")) {
            Matcher m = ROW.matcher(line.strip());
            if (!m.matches()) {
                continue;
            }
            int score = Integer.parseInt(m.group(2));
            ScoreArea.fromLabel(m.group(1))
                    .filter(area -> score <= 10 && seen.add(area))
                    .ifPresent(area -> rows.add(new Row(area, score, m.group(3).isBlank() ? null
                            : truncate(m.group(3)))));
        }
        return rows;
    }

    private static String truncate(String reason) {
        return reason.length() <= 500 ? reason : reason.substring(0, 497) + "...";
    }
}
