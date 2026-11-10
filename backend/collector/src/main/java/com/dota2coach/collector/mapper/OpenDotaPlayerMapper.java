package com.dota2coach.collector.mapper;

import com.dota2coach.collector.dto.opendota.OpenDotaPlayerHeroResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaPlayerResponse;
import com.dota2coach.collector.dto.opendota.OpenDotaWinLossResponse;
import com.dota2coach.core.model.HeroStats;
import com.dota2coach.core.model.PlayerSummary;
import com.dota2coach.core.model.SteamId;
import java.util.Comparator;
import java.util.List;

/** Converte respostas da OpenDota no modelo do domínio. */
public final class OpenDotaPlayerMapper {

    private OpenDotaPlayerMapper() {}

    public static PlayerSummary toPlayerSummary(
            long accountId,
            OpenDotaPlayerResponse player,
            OpenDotaWinLossResponse winLoss,
            List<OpenDotaPlayerHeroResponse> heroes,
            int topHeroesLimit) {
        OpenDotaPlayerResponse.Profile profile = player.profile();
        List<HeroStats> topHeroes = heroes.stream()
                .filter(h -> h.games() > 0)
                .sorted(Comparator.comparingInt(OpenDotaPlayerHeroResponse::games).reversed())
                .limit(topHeroesLimit)
                .map(h -> new HeroStats(h.heroId(), h.games(), h.win()))
                .toList();
        return new PlayerSummary(
                SteamId.fromAccountId(accountId),
                profile.personaName(),
                profile.avatarFull(),
                player.rankTier(),
                winLoss == null ? 0 : winLoss.win(),
                winLoss == null ? 0 : winLoss.lose(),
                topHeroes);
    }
}
