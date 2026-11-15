package com.dota2coach.core.model;

import com.dota2coach.core.enums.FocusMetric;
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

/**
 * Meta numérica de um foco do diagnóstico. O valor de partida é calculado pelo app no Raio-X do diagnóstico, não
 * copiado do texto da IA.
 */
@Entity
@Table(name = "focus_goal")
public class FocusGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private CoachDiagnosis diagnosis;

    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric", nullable = false, length = 40)
    private FocusMetric metric;

    @Column(name = "hero_name", length = 60)
    private String heroName;

    @Column(name = "baseline")
    private Double baseline;

    @Column(name = "target", nullable = false)
    private double target;

    protected FocusGoal() {}

    public FocusGoal(CoachDiagnosis diagnosis, int ordinal, String title, FocusMetric metric, String heroName,
            Double baseline, double target) {
        this.diagnosis = diagnosis;
        this.ordinal = ordinal;
        this.title = title;
        this.metric = metric;
        this.heroName = heroName;
        this.baseline = baseline;
        this.target = target;
    }

    public Long getId() {
        return id;
    }

    public CoachDiagnosis getDiagnosis() {
        return diagnosis;
    }

    /** Ordem do foco no diagnóstico, começando em 1. */
    public int getOrdinal() {
        return ordinal;
    }

    public String getTitle() {
        return title;
    }

    public FocusMetric getMetric() {
        return metric;
    }

    /** Herói da meta, ou nulo quando vale para todas as partidas. */
    public String getHeroName() {
        return heroName;
    }

    /** Valor no Raio-X do diagnóstico, ou nulo sem dado. */
    public Double getBaseline() {
        return baseline;
    }

    public double getTarget() {
        return target;
    }
}
