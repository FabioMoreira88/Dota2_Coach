package com.dota2coach.core.model;

import java.util.Map;

/**
 * Partidas públicas recentes de um herói contra cada outro herói.
 *
 * @param againstHero partidas e vitórias do herói contra o herói adversário, pelo id do adversário
 */
public record HeroMatchups(int heroId, Map<Integer, Record> againstHero) {

    public HeroMatchups {
        againstHero = Map.copyOf(againstHero);
    }

    public static HeroMatchups empty(int heroId) {
        return new HeroMatchups(heroId, Map.of());
    }

    public record Record(int games, int wins) {

        public double winRate() {
            return games == 0 ? 0 : wins / (double) games;
        }
    }
}
