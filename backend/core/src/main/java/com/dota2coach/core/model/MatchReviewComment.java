package com.dota2coach.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** Comentário que a IA escreveu para uma análise de partida salva. */
@Entity
@Table(name = "match_review_comment")
public class MatchReviewComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private MatchReview review;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "input_tokens", nullable = false)
    private long inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private long outputTokens;

    @Column(name = "cost_usd", nullable = false, precision = 10, scale = 4)
    private BigDecimal costUsd;

    @Column(name = "text", nullable = false, columnDefinition = "text")
    private String text;

    protected MatchReviewComment() {}

    public MatchReviewComment(
            MatchReview review, String provider, String model, Instant generatedAt, long inputTokens,
            long outputTokens, BigDecimal costUsd, String text) {
        this.review = review;
        this.provider = provider;
        this.model = model;
        this.generatedAt = generatedAt;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.costUsd = costUsd;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public MatchReview getReview() {
        return review;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public long getInputTokens() {
        return inputTokens;
    }

    public long getOutputTokens() {
        return outputTokens;
    }

    public BigDecimal getCostUsd() {
        return costUsd;
    }

    public String getText() {
        return text;
    }
}
