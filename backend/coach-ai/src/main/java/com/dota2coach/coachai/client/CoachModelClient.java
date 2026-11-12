package com.dota2coach.coachai.client;

import com.dota2coach.coachai.prompt.CoachPrompt;

/** Um modelo de IA capaz de escrever o texto do coach. */
public interface CoachModelClient {

    /** Nome curto para relatórios (ex.: "claude", "llama-70b-groq"). */
    String name();

    /**
     * Gera o texto do coach.
     *
     * @throws CoachModelException se o provedor falhar ou recusar o pedido
     */
    CoachReply generate(CoachPrompt prompt);
}
