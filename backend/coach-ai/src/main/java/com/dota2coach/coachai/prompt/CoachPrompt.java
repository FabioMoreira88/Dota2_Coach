package com.dota2coach.coachai.prompt;

/** Prompt enviado a um modelo: instruções fixas (system) e a mensagem com os dados do jogador. */
public record CoachPrompt(String system, String user) {}
