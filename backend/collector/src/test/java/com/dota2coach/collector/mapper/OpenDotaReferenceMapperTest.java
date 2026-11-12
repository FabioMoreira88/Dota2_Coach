package com.dota2coach.collector.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.dota2coach.collector.dto.opendota.OpenDotaHeroStatsResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaItemConstant;
import com.dota2coach.collector.dto.opendota.OpenDotaItemPopularityResponse;
import com.dota2coach.core.enums.GamePhase;
import com.dota2coach.core.enums.RankBracket;
import com.dota2coach.core.model.HeroItemPopularity;
import com.dota2coach.core.model.HeroMeta;
import com.dota2coach.core.model.Item;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenDotaReferenceMapperTest {

    @Test
    void ignoraItensSemIdEUsaNomeInternoQuandoFaltaONome() {
        Map<Integer, Item> items = OpenDotaReferenceMapper.toItems(Map.of(
                "blink", new OpenDotaItemConstant(1, "Blink Dagger", 2250),
                "recipe_sem_id", new OpenDotaItemConstant(0, "Recipe", 0),
                "item_novo", new OpenDotaItemConstant(999, null, null)));

        assertThat(items).containsOnlyKeys(1, 999);
        assertThat(items.get(1)).isEqualTo(new Item(1, "blink", "Blink Dagger", 2250));
        assertThat(items.get(999).name()).isEqualTo("item_novo");
        assertThat(items.get(999).cost()).isZero();
    }

    @Test
    void montaMetaPorFaixaDeMedalha() {
        HeroMeta meta = OpenDotaReferenceMapper.toHeroMeta(new OpenDotaHeroStatsResponse(
                35, Map.of(5, 5000, 8, 800), Map.of(5, 2600, 8, 380), 12, 5));

        assertThat(meta.in(RankBracket.LEGEND).winRate()).isEqualTo(0.52);
        assertThat(meta.in(RankBracket.HERALD).picks()).isZero();
        assertThat(meta.pro().picks()).isEqualTo(12);
    }

    @Test
    void ordenaItensPopularesPorFase() {
        HeroItemPopularity popularity = OpenDotaReferenceMapper.toItemPopularity(35,
                new OpenDotaItemPopularityResponse(null, Map.of(63, 700), Map.of(166, 400, 236, 350, 116, 200), null));

        assertThat(popularity.top(GamePhase.MID, 2)).containsExactly(166, 236);
        assertThat(popularity.top(GamePhase.START, 3)).isEmpty();
    }

    @Test
    void popularidadeNulaViraVazia() {
        assertThat(OpenDotaReferenceMapper.toItemPopularity(1, null).byPhase()).isEmpty();
    }
}
