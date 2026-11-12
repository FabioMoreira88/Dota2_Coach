package com.dota2coach.core.model;

import com.dota2coach.core.enums.RankBracket;
import java.util.Map;

/** Escolhas e vitórias de um herói em partidas públicas recentes, por faixa de medalha. */
public record HeroMeta(int heroId, Map<RankBracket, PickWin> byBracket, PickWin pro) {

    public HeroMeta {
        byBracket = Map.copyOf(byBracket);
    }

    public PickWin in(RankBracket bracket) {
        return byBracket.getOrDefault(bracket, PickWin.NONE);
    }

    /** Quantas vezes o herói foi escolhido e quantas venceu. */
    public record PickWin(int picks, int wins) {

        public static final PickWin NONE = new PickWin(0, 0);

        public double winRate() {
            return picks == 0 ? 0.0 : (double) wins / picks;
        }
    }
}
