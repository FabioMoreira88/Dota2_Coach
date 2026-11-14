package com.dota2coach.core.service;

import com.dota2coach.core.model.MatchReview;
import com.dota2coach.core.model.MatchReviewComment;
import com.dota2coach.core.repository.MatchReviewCommentRepository;
import com.dota2coach.core.repository.MatchReviewRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Guarda e lê as análises de partida e os comentários do coach de cada conta. */
@Service
public class MatchReviewService {

    private final MatchReviewRepository reviews;
    private final MatchReviewCommentRepository comments;

    public MatchReviewService(MatchReviewRepository reviews, MatchReviewCommentRepository comments) {
        this.reviews = reviews;
        this.comments = comments;
    }

    @Transactional
    public MatchReview saveReview(MatchReview review) {
        return reviews.save(review);
    }

    @Transactional
    public MatchReviewComment saveComment(MatchReviewComment comment) {
        return comments.save(comment);
    }

    /** Análises da conta, da mais recente para a mais antiga. */
    @Transactional(readOnly = true)
    public List<MatchReview> history(long accountId) {
        return reviews.findByAccountIdOrderByAnalyzedAtDesc(accountId);
    }

    @Transactional(readOnly = true)
    public List<MatchReviewComment> comments(long reviewId) {
        return comments.findByReviewIdOrderByGeneratedAtDesc(reviewId);
    }
}
