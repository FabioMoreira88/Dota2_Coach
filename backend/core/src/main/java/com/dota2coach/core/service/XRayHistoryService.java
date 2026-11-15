package com.dota2coach.core.service;

import com.dota2coach.core.model.CoachDiagnosis;
import com.dota2coach.core.model.FocusGoal;
import com.dota2coach.core.model.XRayReport;
import com.dota2coach.core.repository.CoachDiagnosisRepository;
import com.dota2coach.core.repository.FocusGoalRepository;
import com.dota2coach.core.repository.XRayReportRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Guarda e lê o histórico de Raio-X e diagnósticos de cada conta. */
@Service
public class XRayHistoryService {

    private final XRayReportRepository reports;
    private final CoachDiagnosisRepository diagnoses;
    private final FocusGoalRepository goals;

    public XRayHistoryService(XRayReportRepository reports, CoachDiagnosisRepository diagnoses,
            FocusGoalRepository goals) {
        this.reports = reports;
        this.diagnoses = diagnoses;
        this.goals = goals;
    }

    @Transactional
    public XRayReport saveReport(XRayReport report) {
        return reports.save(report);
    }

    @Transactional
    public CoachDiagnosis saveDiagnosis(CoachDiagnosis diagnosis) {
        return diagnoses.save(diagnosis);
    }

    /** Raio-X da conta, do mais recente para o mais antigo. */
    @Transactional(readOnly = true)
    public List<XRayReport> history(long accountId) {
        return reports.findByAccountIdOrderByCollectedAtDesc(accountId);
    }

    @Transactional(readOnly = true)
    public Optional<XRayReport> latest(long accountId) {
        return reports.findFirstByAccountIdOrderByCollectedAtDesc(accountId);
    }

    @Transactional(readOnly = true)
    public long count(long accountId) {
        return reports.countByAccountId(accountId);
    }

    @Transactional
    public List<FocusGoal> saveGoals(List<FocusGoal> focusGoals) {
        return goals.saveAll(focusGoals);
    }

    /** Metas dos focos do diagnóstico, na ordem em que a IA escreveu. */
    @Transactional(readOnly = true)
    public List<FocusGoal> goals(long diagnosisId) {
        return goals.findByDiagnosisIdOrderByOrdinal(diagnosisId);
    }

    @Transactional(readOnly = true)
    public List<CoachDiagnosis> diagnoses(long reportId) {
        return diagnoses.findByReportIdOrderByGeneratedAtDesc(reportId);
    }
}
