package com.dota2coach.coachai.enums;

/** Como falar com um provedor de IA. */
public enum ProviderType {
    /** API da Anthropic (Claude), pelo SDK oficial. */
    ANTHROPIC,
    /** Qualquer serviço no formato de chat da OpenAI: Groq, OpenRouter, Ollama local. */
    OPENAI_COMPATIBLE
}
