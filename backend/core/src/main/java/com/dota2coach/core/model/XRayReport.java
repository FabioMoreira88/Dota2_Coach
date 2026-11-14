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
 * Um Raio-X salvo. Os números ficam em JSON para que o formato do Raio-X possa crescer sem migração; o Markdown é o
 * relatório exatamente como o jogador e a IA leram.
 */
@Entity
@Table(name = "xray_report")
public class XRayReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", length = 20)
    private Position position;

    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    @Column(name = "recent_window", nullable = false)
    private int recentWindow;

    @Column(name = "metrics_json", nullable = false, columnDefinition = "text")
    private String metricsJson;

    @Column(name = "markdown", nullable = false, columnDefinition = "text")
    private String markdown;

    protected XRayReport() {}

    public XRayReport(
            long accountId, Position position, Instant collectedAt, int recentWindow, String metricsJson,
            String markdown) {
        this.accountId = accountId;
        this.position = position;
        this.collectedAt = collectedAt;
        this.recentWindow = recentWindow;
        this.metricsJson = metricsJson;
        this.markdown = markdown;
    }

    public Long getId() {
        return id;
    }

    public long getAccountId() {
        return accountId;
    }

    /** Posição informada pelo jogador, ou nulo se ele não informou. */
    public Position getPosition() {
        return position;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public int getRecentWindow() {
        return recentWindow;
    }

    public String getMetricsJson() {
        return metricsJson;
    }

    public String getMarkdown() {
        return markdown;
    }
}
