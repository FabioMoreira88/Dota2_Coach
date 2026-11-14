package com.dota2coach.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dota2coach.core.enums.Position;
import com.dota2coach.core.model.MatchReview;
import com.dota2coach.core.model.MatchReviewComment;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/** Roda as migrações do Flyway no H2 e confere que as entidades batem com as tabelas (ddl-auto=validate). */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Import(MatchReviewService.class)
class MatchReviewServiceTest {

    private static final long ACCOUNT = 34839344L;

    @Autowired
    private MatchReviewService reviews;

    private MatchReview review(long matchId, String analyzedAt) {
        return reviews.saveReview(new MatchReview(ACCOUNT, matchId, Position.MID, Instant.parse(analyzedAt), true,
                "{}", "# Análise"));
    }

    @Test
    void historicoDaContaDaMaisRecenteParaAMaisAntiga() {
        review(1L, "2026-11-13T12:00:00Z");
        review(2L, "2026-11-14T12:00:00Z");
        reviews.saveReview(new MatchReview(1L, 3L, null, Instant.parse("2026-11-14T13:00:00Z"), false, "{}", "#"));

        assertThat(reviews.history(ACCOUNT)).extracting(MatchReview::getMatchId).containsExactly(2L, 1L);
    }

    @Test
    void comentarioFicaLigadoAAnalise() {
        MatchReview review = review(1L, "2026-11-14T12:00:00Z");
        reviews.saveComment(new MatchReviewComment(review, "claude", "claude-opus-5-5",
                Instant.parse("2026-11-14T12:01:00Z"), 4000, 2000, new BigDecimal("0.0560"), "## Rotas"));

        assertThat(reviews.comments(review.getId())).singleElement().satisfies(c -> {
            assertThat(c.getText()).isEqualTo("## Rotas");
            assertThat(c.getCostUsd()).isEqualByComparingTo("0.0560");
        });
    }
}
