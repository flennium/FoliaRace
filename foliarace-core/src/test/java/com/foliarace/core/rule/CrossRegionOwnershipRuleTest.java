package com.foliarace.core.rule;

import com.foliarace.core.context.ExecutionContext;
import com.foliarace.core.context.ExecutionContextType;
import com.foliarace.core.evidence.Confidence;
import com.foliarace.core.evidence.OwnershipEvidence;
import com.foliarace.core.evidence.OwnershipKey;
import com.foliarace.core.evidence.OwnershipType;
import com.foliarace.core.evidence.ResolutionSource;
import com.foliarace.core.observation.CallSite;
import com.foliarace.core.observation.Observation;
import com.foliarace.core.observation.OperationCategory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossRegionOwnershipRuleTest {
    private static final Instant NOW = Instant.parse("2026-08-12T00:00:00Z");

    @Test
    void reportsAuthoritativeCrossRegionAccess() {
        Optional<?> result = new CrossRegionOwnershipRule().evaluate(observation("region-a", "region-b"));

        assertTrue(result.isPresent());
        assertEquals(Confidence.CONFIRMED, ((com.foliarace.core.finding.FindingDraft) result.orElseThrow()).confidence());
    }

    @Test
    void ignoresSameRegionAccess() {
        assertTrue(new CrossRegionOwnershipRule().evaluate(observation("region-a", "region-a")).isEmpty());
    }

    @Test
    void doesNotConfirmUnknownOwnership() {
        Observation unknown = Observation.create(
                NOW,
                "fixture-plugin",
                OperationCategory.BLOCK_ACCESS,
                new ExecutionContext(ExecutionContextType.REGION, "region-a", "region-thread", NOW),
                OwnershipEvidence.unknown(NOW),
                new CallSite("fixture.Plugin#run", java.util.List.of("fixture.Plugin#run"))
        );

        assertTrue(new CrossRegionOwnershipRule().evaluate(unknown).isEmpty());
    }

    private static Observation observation(String executionRegion, String targetRegion) {
        return Observation.create(
                NOW,
                "fixture-plugin",
                OperationCategory.BLOCK_ACCESS,
                new ExecutionContext(ExecutionContextType.REGION, executionRegion, "region-thread", NOW),
                new OwnershipEvidence(
                        new OwnershipKey(OwnershipType.REGION, targetRegion),
                        ResolutionSource.AUTHORITATIVE_API,
                        Confidence.CONFIRMED,
                        NOW
                ),
                new CallSite("fixture.Plugin#run", java.util.List.of("fixture.Plugin#run"))
        );
    }
}
