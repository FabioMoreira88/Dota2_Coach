package com.dota2coach.coachai.client;

import java.time.Duration;

/**
 * Resposta de um modelo ao prompt do coach.
 *
 * @param model modelo que respondeu, como o provedor informou
 * @param inputTokens tokens de entrada cobrados, ou 0 se o provedor não informou
 * @param outputTokens tokens de saída cobrados, ou 0 se o provedor não informou
 */
public record CoachReply(String text, String model, long inputTokens, long outputTokens, Duration elapsed) {}
