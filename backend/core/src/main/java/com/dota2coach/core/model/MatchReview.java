package com.dota2coach.core.model;

import com.dota2coach.core.enums.Position;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Uma análise de partida escolhida, salva. Os números ficam em JSON, como no {@link XRayReport}; o Markdown é o
 * relatório exatamente como o jogador e a IA leram.
 */
@Entity
@Table(name = "match_review")
public class MatchReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private long accountId;

    @Column(name = "match_id", nullable = false)
    private long matchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", length = 20)
    private Position position;

    @Column(name = "analyzed_at", nullable = false)
    private Instant analyzedAt;

    @Column(name = "replay_parsed", nullable = false)
    private boolean replayParsed;

    @Column(name = "metrics_json", nullable = false, columnDefinition = "text")
    private String metricsJson;

    @Column(name = "markdown", nullable = false, columnDefinition = "text")
    private String markdown;

    protected MatchReview() {}

    public MatchReview(
            long accountId, long matchId, Position position, Instant analyzedAt, boolean replayParsed,
            String metricsJson, String markdown) {
        this.accountId = accountId;
        this.matchId = matchId;
        this.position = position;
        this.analyzedAt = analyzedAt;
        this.replayParsed = replayParsed;
        this.metricsJson = metricsJson;
        this.markdown = markdown;
    }

    public Long getId() {
        return id;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getMatchId() {
        return matchId;
    }

    public Position getPosition() {
        return position;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public boolean isReplayParsed() {
        return replayParsed;
    }

    public String getMetricsJson() {
        return metricsJson;
    }

    public String getMarkdown() {
        return markdown;
    }
}
