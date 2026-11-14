package com.dota2coach.analysis.metric;

import static com.dota2coach.analysis.support.Matches.ACCOUNT;
import static com.dota2coach.analysis.support.Matches.sniperMid;
import static org.assertj.core.api.Assertions.assertThat;

import com.dota2coach.analysis.metric.MatchAnalysis.FightLine;
import com.dota2coach.analysis.metric.MatchAnalysis.ReplayMoment;
import com.dota2coach.core.model.MatchPlayer;
import com.dota2coach.core.model.MatchSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchTimelineCalculatorTest {

    @Test
    void mortesEmLutasForaDasMaioresCompletamOsMomentos() {
        MatchSnapshot snapshot = sniperMid(true);
        MatchPlayer me = snapshot.match().players().stream()
                .filter(p -> Long.valueOf(ACCOUNT).equals(p.accountId())).findFirst().orElseThrow();
        List<FightLine> fights = List.of(
                fight(600, 640, 3000), fight(1000, 1040, -2500), fight(1500, 1540, 2000),
                fight(1880, 1930, -100));

        List<ReplayMoment> moments = MatchTimelineCalculator.replayMoments(snapshot, me, fights);

        assertThat(moments).as("a luta das 31:20 não está entre as 3 maiores, mas você morreu nela")
                .containsExactly(
                        new ReplayMoment(270, 305, "você morreu fora de luta para Axe"),
                        new ReplayMoment(600, 640, "luta com +3000 de ouro para seu time; você participou"),
                        new ReplayMoment(1000, 1040, "luta com -2500 de ouro para seu time; você participou"),
                        new ReplayMoment(1500, 1540, "luta com +2000 de ouro para seu time; você participou"),
                        new ReplayMoment(1880, 1930, "você morreu numa luta com -100 de ouro para seu time"));
    }

    private static FightLine fight(int start, int end, int swing) {
        return new FightLine(start, end, swing, List.of(), List.of(), true, false, 0);
    }
}
