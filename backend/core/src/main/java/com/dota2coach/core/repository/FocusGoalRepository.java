package com.dota2coach.core.repository;

import com.dota2coach.core.model.FocusGoal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FocusGoalRepository extends JpaRepository<FocusGoal, Long> {

    List<FocusGoal> findByDiagnosisIdOrderByOrdinal(long diagnosisId);
}
