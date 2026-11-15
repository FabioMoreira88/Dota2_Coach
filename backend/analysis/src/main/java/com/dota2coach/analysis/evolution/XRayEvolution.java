package com.dota2coach.analysis.evolution;

import com.dota2coach.core.enums.Position;
import java.time.Instant;
import java.util.List;

/**
 * O Raio-X atual comparado com o anterior da mesma conta.
 *
 * @param previousAt quando o Raio-X anterior foi gerado
 * @param previousPosition posição informada no Raio-X anterior, ou nulo
 * @param newMatches partidas significativas jogadas entre os dois Raio-X
 * @param recentWindow tamanho do recorte recente; com menos partidas novas que isso, os recortes se sobrepõem
 * @param changes métricas comparadas, na ordem do relatório
 */
public record XRayEvolution(
        Instant previousAt,
        Position previousPosition,
        int newMatches,
        int recentWindow,
        List<Change> changes) {

    public XRayEvolution {
        changes = List.copyOf(changes);
    }

    /**
     * @param group bloco do relatório, ex.: "Fundamentos (últimas 20)"
     * @param before valor no Raio-X anterior, ou nulo sem dado
     * @param after valor no Raio-X atual, ou nulo sem dado
     */
    public record Change(String group, String label, Double before, Double after, Format format, Trend trend) {}

    /** Como mostrar o número. */
    public enum Format {
        PERCENT,
        DECIMAL,
        SECONDS
    }

    public enum Trend {
        IMPROVED("melhorou"),
        SAME("igual"),
        WORSE("piorou"),
        NO_DATA("sem dados");

        private final String label;

        Trend(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
