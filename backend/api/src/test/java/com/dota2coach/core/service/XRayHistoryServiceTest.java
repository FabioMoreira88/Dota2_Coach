package com.dota2coach.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dota2coach.core.enums.Position;
import com.dota2coach.core.model.CoachDiagnosis;
import com.dota2coach.core.model.XRayReport;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/** Roda as migrações do Flyway no H2 e confere que as entidades batem com as tabelas (ddl-auto=validate). */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Import(XRayHistoryService.class)
class XRayHistoryServiceTest {

    private static final long ACCOUNT = 34839344L;

    @Autowired
    private XRayHistoryService history;

    private XRayReport report(Position position, String collectedAt) {
        return history.saveReport(new XRayReport(ACCOUNT, position, Instant.parse(collectedAt), 100, "{}", "# Raio-X"));
    }

    @Test
    void historicoDaContaDoMaisRecenteParaOMaisAntigo() {
        report(Position.MID, "2026-11-11T12:00:00Z");
        XRayReport latest = report(Position.CARRY, "2026-11-13T12:00:00Z");
        history.saveReport(new XRayReport(1L, null, Instant.parse("2026-11-14T12:00:00Z"), 100, "{}", "# outro"));

        assertThat(history.history(ACCOUNT)).extracting(XRayReport::getPosition)
                .containsExactly(Position.CARRY, Position.MID);
        assertThat(history.latest(ACCOUNT)).get().extracting(XRayReport::getId).isEqualTo(latest.getId());
        assertThat(history.count(ACCOUNT)).isEqualTo(2);
        assertThat(history.latest(999L)).isEmpty();
    }

    @Test
    void diagnosticoFicaLigadoAoRaioX() {
        XRayReport report = report(Position.MID, "2026-11-13T12:00:00Z");
        history.saveDiagnosis(new CoachDiagnosis(report, "claude", "claude-opus-5-5",
                Instant.parse("2026-11-13T12:01:00Z"), 5332, 4537, new BigDecimal("0.1121"), "## Foco 1"));

        assertThat(history.diagnoses(report.getId())).singleElement().satisfies(d -> {
            assertThat(d.getModel()).isEqualTo("claude-opus-5-5");
            assertThat(d.getCostUsd()).isEqualByComparingTo("0.1121");
            assertThat(d.getText()).isEqualTo("## Foco 1");
        });
    }
}
