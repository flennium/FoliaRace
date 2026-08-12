package com.foliarace.core.rule;

import com.foliarace.core.context.ExecutionContext;
import com.foliarace.core.context.ExecutionContextType;
import com.foliarace.core.evidence.Confidence;
import com.foliarace.core.evidence.OwnershipEvidence;
import com.foliarace.core.evidence.OwnershipKey;
import com.foliarace.core.evidence.OwnershipType;
import com.foliarace.core.evidence.ResolutionSource;
import com.foliarace.core.finding.RemediationCategory;
import com.foliarace.core.observation.CallSite;
import com.foliarace.core.observation.Observation;
import com.foliarace.core.observation.OperationCategory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdditionalDetectorRulesTest {
    private static final Instant NOW = Instant.parse("2026-08-12T00:00:00Z");

    @Test
    void entityRuleRequiresAuthoritativeMismatch() {
        Observation observation = observation(
                OperationCategory.ENTITY_ACCESS,
                new ExecutionContext(ExecutionContextType.REGION, "region-a", "region", NOW),
                OwnershipEvidence.authoritativeCurrentContextCheck(false, NOW),
                Map.of()
        );

        var finding = new CrossEntityOwnershipRule().evaluate(observation).orElseThrow();
        assertEquals(Confidence.CONFIRMED, finding.confidence());
    }

    @Test
    void asyncRuleReportsProbableRestrictedAccess() {
        Observation observation = observation(
                OperationCategory.BLOCK_ACCESS,
                new ExecutionContext(ExecutionContextType.ASYNC, "", "Folia Async Scheduler Thread #1", NOW),
                OwnershipEvidence.unknown(NOW),
                Map.of()
        );

        var finding = new AsyncServerStateRule().evaluate(observation).orElseThrow();
        assertEquals(Confidence.PROBABLE, finding.confidence());
    }

    @Test
    void schedulerRuleDistinguishesLocationAndEntityTargets() {
        Observation location = observation(
                OperationCategory.SCHEDULER_SUBMISSION,
                ExecutionContext.unknown(NOW, "region"),
                OwnershipEvidence.unknown(NOW),
                Map.of("scheduler", "global", "targetKind", "location")
        );
        Observation entity = observation(
                OperationCategory.SCHEDULER_SUBMISSION,
                ExecutionContext.unknown(NOW, "region"),
                OwnershipEvidence.unknown(NOW),
                Map.of("scheduler", "region", "targetKind", "entity")
        );

        assertEquals(RemediationCategory.USE_REGION_SCHEDULER, new SchedulerMisuseRule().evaluate(location).orElseThrow().remediation());
        assertEquals(RemediationCategory.USE_ENTITY_SCHEDULER, new SchedulerMisuseRule().evaluate(entity).orElseThrow().remediation());
    }

    @Test
    void catalogRejectsUnknownDetectors() {
        assertTrue(DetectorCatalog.ids().containsAll(Set.of(
                CrossRegionOwnershipRule.ID,
                CrossEntityOwnershipRule.ID,
                AsyncServerStateRule.ID,
                SchedulerMisuseRule.ID
        )));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> DetectorCatalog.create(Set.of("missing-detector")));
    }

    private static Observation observation(
            OperationCategory category,
            ExecutionContext context,
            OwnershipEvidence ownership,
            Map<String, String> metadata
    ) {
        return new Observation(
                java.util.UUID.randomUUID(), NOW, "fixture", "fixture", category, context, ownership,
                null, new CallSite("fixture.Plugin#run", List.of("fixture.Plugin#run")), metadata
        );
    }
}
