package com.dota2coach.core.repository;

import com.dota2coach.core.model.MatchAreaScore;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchAreaScoreRepository extends JpaRepository<MatchAreaScore, Long> {

    List<MatchAreaScore> findByAccountIdOrderByScoredAtDesc(long accountId);
}
