package com.dota2coach.coachai.service;

import com.dota2coach.coachai.client.CoachModelClient;
import com.dota2coach.coachai.client.CoachModelException;
import com.dota2coach.coachai.client.CoachReply;
import com.dota2coach.coachai.prompt.CoachPrompt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manda o mesmo prompt a vários modelos e embaralha as respostas, para o jogador dar nota sem saber qual modelo
 * escreveu cada texto.
 */
public class CoachComparisonService {

    private static final Logger log = LoggerFactory.getLogger(CoachComparisonService.class);

    private final Random random;

    public CoachComparisonService(Random random) {
        this.random = random;
    }

    /**
     * Roda o prompt em cada cliente. Uma falha em um modelo não impede os outros.
     *
     * @param pricing preço de cada cliente pelo nome; ausente conta como grátis
     * @return uma entrada por cliente, em ordem embaralhada, com as letras A, B, C...
     */
    public List<Entry> compare(CoachPrompt prompt, List<CoachModelClient> clients, Map<String, ProviderPricing> pricing) {
        List<CoachModelClient> shuffled = new ArrayList<>(clients);
        Collections.shuffle(shuffled, random);
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < shuffled.size(); i++) {
            CoachModelClient client = shuffled.get(i);
            String label = String.valueOf((char) ('A' + i));
            log.info("Comparativo de IA: gerando texto {} com {}...", label, client.name());
            try {
                CoachReply reply = client.generate(prompt);
                double cost = pricing.getOrDefault(client.name(), ProviderPricing.FREE)
                        .cost(reply.inputTokens(), reply.outputTokens());
                entries.add(new Entry(label, client.name(), reply, null, cost));
            } catch (CoachModelException e) {
                log.warn("Comparativo de IA: {} falhou: {}", client.name(), e.getMessage());
                entries.add(new Entry(label, client.name(), null, e.getMessage(), 0));
            }
        }
        return entries;
    }

    /**
     * Resultado de um modelo.
     *
     * @param reply resposta, ou nulo se o modelo falhou
     * @param error motivo da falha, ou nulo
     * @param costUsd custo estimado em dólares
     */
    public record Entry(String label, String provider, CoachReply reply, String error, double costUsd) {

        public boolean succeeded() {
            return reply != null;
        }
    }
}
