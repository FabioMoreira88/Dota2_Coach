package com.dota2coach.coachai.prompt;

import com.dota2coach.core.enums.FocusMetric;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bloco de metas que a IA escreve no fim do diagnóstico, em JSON dentro de um bloco de código {@code metas}. O
 * bloco sai do texto que o jogador lê; as metas válidas viram dados.
 */
public final class FocusGoalBlock {

    static final int MAX_GOALS = 3;
    private static final Pattern BLOCK = Pattern.compile("```metas\\s*\\n(.*?)```", Pattern.DOTALL);
    private static final ObjectMapper JSON = new ObjectMapper();

    private FocusGoalBlock() {}

    /**
     * @param text diagnóstico sem o bloco de metas
     * @param goals metas com métrica conhecida e número, na ordem do texto (no máximo 3)
     * @param ignored metas descartadas, com o motivo, para o log
     */
    public record Parsed(String text, List<Draft> goals, List<String> ignored) {

        public Parsed {
            goals = List.copyOf(goals);
            ignored = List.copyOf(ignored);
        }
    }

    /** @param heroName herói da meta, ou nulo quando vale para todas as partidas */
    public record Draft(String title, FocusMetric metric, String heroName, double target) {}

    public static Parsed parse(String diagnosis) {
        String source = diagnosis == null ? "" : diagnosis;
        Matcher m = BLOCK.matcher(source);
        if (!m.find()) {
            return new Parsed(source.strip(), List.of(), List.of());
        }
        String text = (source.substring(0, m.start()) + source.substring(m.end())).strip();
        List<Draft> goals = new ArrayList<>();
        List<String> ignored = new ArrayList<>();
        JsonNode focos;
        try {
            focos = JSON.readTree(m.group(1)).path("focos");
        } catch (Exception e) {
            return new Parsed(text, List.of(), List.of("bloco de metas não é um JSON válido"));
        }
        for (JsonNode f : focos) {
            String title = f.path("titulo").asText("").strip();
            String key = f.path("metrica").asText("");
            JsonNode target = f.path("meta");
            var metric = FocusMetric.fromKey(key);
            if (metric.isEmpty()) {
                ignored.add("métrica desconhecida '" + key + "'");
            } else if (!target.isNumber()) {
                ignored.add("meta sem número em '" + key + "'");
            } else if (goals.size() >= MAX_GOALS) {
                ignored.add("mais de " + MAX_GOALS + " metas");
            } else {
                String hero = f.path("heroi").isTextual() && !f.path("heroi").asText().isBlank()
                        ? f.path("heroi").asText().strip() : null;
                goals.add(new Draft(title.isEmpty() ? metric.get().label() : title, metric.get(), hero,
                        target.asDouble()));
            }
        }
        return new Parsed(text, goals, ignored);
    }

    /** Lista de métricas para o prompt: chave e o que ela mede. */
    static String catalog() {
        StringBuilder s = new StringBuilder();
        for (FocusMetric metric : FocusMetric.values()) {
            s.append("  - ").append(metric.key()).append(": ").append(metric.label())
                    .append(metric.higherIsBetter() ? " (maior é melhor)" : " (menor é melhor)")
                    .append(metric.unit() == FocusMetric.Unit.PERCENT ? ", de 0 a 1" : "").append('\n');
        }
        return s.toString();
    }
}
