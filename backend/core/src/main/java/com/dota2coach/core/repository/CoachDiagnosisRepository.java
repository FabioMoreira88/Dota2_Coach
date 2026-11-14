package com.dota2coach.core.repository;

import com.dota2coach.core.model.CoachDiagnosis;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachDiagnosisRepository extends JpaRepository<CoachDiagnosis, Long> {

    List<CoachDiagnosis> findByReportIdOrderByGeneratedAtDesc(long reportId);
}
