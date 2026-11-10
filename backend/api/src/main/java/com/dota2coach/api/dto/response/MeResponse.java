package com.dota2coach.api.dto.response;

/**
 * Dados do jogador logado.
 *
 * @param profile resumo da OpenDota, ou null se a OpenDota ainda não conhece a conta
 */
public record MeResponse(String steamId64, long accountId, PlayerSummaryResponse profile) {}
