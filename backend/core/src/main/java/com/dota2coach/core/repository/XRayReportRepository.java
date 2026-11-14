package com.dota2coach.core.repository;

import com.dota2coach.core.model.XRayReport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface XRayReportRepository extends JpaRepository<XRayReport, Long> {

    List<XRayReport> findByAccountIdOrderByCollectedAtDesc(long accountId);

    Optional<XRayReport> findFirstByAccountIdOrderByCollectedAtDesc(long accountId);

    long countByAccountId(long accountId);
}
