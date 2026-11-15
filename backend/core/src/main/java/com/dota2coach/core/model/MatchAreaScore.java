package com.dota2coach.core.model;

import com.dota2coach.core.enums.ScoreArea;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Nota de 0 a 10 que o coach deu a uma área numa partida revisada. Conta e partida ficam aqui para a tendência. */
@Entity
@Table(name = "match_area_score")
public class MatchAreaScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private MatchReviewComment comment;

    @Column(name = "account_id", nullable = false)
    private long accountId;

    @Column(name = "match_id", nullable = false)
    private long matchId;

    @Column(name = "scored_at", nullable = false)
    private Instant scoredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "area", nullable = false, length = 20)
    private ScoreArea area;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "reason", length = 500)
    private String reason;

    protected MatchAreaScore() {}

    public MatchAreaScore(MatchReviewComment comment, long accountId, long matchId, Instant scoredAt, ScoreArea area,
            int score, String reason) {
        this.comment = comment;
        this.accountId = accountId;
        this.matchId = matchId;
        this.scoredAt = scoredAt;
        this.area = area;
        this.score = score;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public MatchReviewComment getComment() {
        return comment;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getMatchId() {
        return matchId;
    }

    public Instant getScoredAt() {
        return scoredAt;
    }

    public ScoreArea getArea() {
        return area;
    }

    public int getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }
}
