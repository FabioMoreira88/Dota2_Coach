package com.dota2coach.analysis.evolution;

import com.dota2coach.core.enums.FocusMetric;

/**
 * Uma meta de foco conferida com os números do app.
 *
 * @param heroName herói da meta, ou nulo quando vale para todas as partidas
 * @param baseline valor no Raio-X do diagnóstico, ou nulo
 * @param current valor agora (no Raio-X novo ou na partida), ou nulo sem dado
 */
public record FocusGoalCheck(
        int ordinal,
        String title,
        FocusMetric metric,
        String heroName,
        Double baseline,
        double target,
        Double current,
        Status status) {

    public enum Status {
        REACHED("atingiu a meta"),
        ADVANCED("avançou, ainda sem a meta"),
        SAME("igual"),
        WORSE("piorou"),
        MISSED("não atingiu"),
        NO_DATA("sem dados"),
        NOT_APPLICABLE("não se aplica");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
