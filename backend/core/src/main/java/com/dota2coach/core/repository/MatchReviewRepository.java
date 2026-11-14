package com.dota2coach.core.repository;

import com.dota2coach.core.model.MatchReview;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchReviewRepository extends JpaRepository<MatchReview, Long> {

    List<MatchReview> findByAccountIdOrderByAnalyzedAtDesc(long accountId);
}
