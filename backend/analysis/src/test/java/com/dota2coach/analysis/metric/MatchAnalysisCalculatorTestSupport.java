package com.dota2coach.analysis.metric;

import static com.dota2coach.analysis.support.Snapshots.SNIPER;
import static com.dota2coach.analysis.support.Snapshots.match;
import static com.dota2coach.analysis.support.Snapshots.snapshot;

import com.dota2coach.core.enums.Position;
import com.dota2coach.core.model.MatchDetail;
import com.dota2coach.core.model.MatchRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Raio-X de teste para comparar a partida com o padrão do jogador. */
public final class MatchAnalysisCalculatorTestSupport {

    private MatchAnalysisCalculatorTestSupport() {}

    /** Raio-X com 3 partidas de Sniper: 40, 41 e 42 last hits aos 10 min, Power Treads aos 10 min, 1 morte. */
    public static ProfileXRay latestXRay() {
        List<MatchRecord> matches = new ArrayList<>();
        Map<Long, MatchDetail> details = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            matches.add(match(i + 1, SNIPER, true, i));
            details.put(i + 1L, new MatchDetail(i + 1, SNIPER, 40 + i, 2,
                    List.of(new MatchDetail.Purchase("power_treads", 600)),
                    List.of(new MatchDetail.Death(300, 30))));
        }
        return new XRayCalculator().calculate(snapshot(53, matches, details), Position.MID);
    }
}
