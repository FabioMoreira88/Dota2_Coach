package com.dota2coach.core.repository;

import com.dota2coach.core.model.MatchReviewComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchReviewCommentRepository extends JpaRepository<MatchReviewComment, Long> {

    List<MatchReviewComment> findByReviewIdOrderByGeneratedAtDesc(long reviewId);
}
