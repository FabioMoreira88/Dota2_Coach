package com.dota2coach.coachai.service;

/** Preço em dólares por milhão de tokens. Zero para modelos locais ou planos grátis. */
public record ProviderPricing(double inputPerMillion, double outputPerMillion) {

    public static final ProviderPricing FREE = new ProviderPricing(0, 0);

    public double cost(long inputTokens, long outputTokens) {
        return (inputTokens * inputPerMillion + outputTokens * outputPerMillion) / 1_000_000.0;
    }
}
