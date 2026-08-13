package com.foliarace.core.finding;

import com.foliarace.core.context.ExecutionContext;
import com.foliarace.core.context.ExecutionContextType;
import com.foliarace.core.evidence.Confidence;
import com.foliarace.core.evidence.OwnershipEvidence;
import com.foliarace.core.evidence.OwnershipKey;
import com.foliarace.core.evidence.OwnershipType;
import com.foliarace.core.evidence.ResolutionSource;
import com.foliarace.core.observation.CallSite;
import com.foliarace.core.observation.Observation;
import com.foliarace.core.observation.ObservationOrigin;
import com.foliarace.core.observation.OperationCategory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FindingFingerprintTest {
    private static final Instant NOW = Instant.parse("2026-08-12T00:00:00Z");

    @Test
    void separatesDistinctExecutionAndTargetOwners() {
        FindingDraft draft = draft();
        Finding first = Finding.from(observation("region-a", "region-x", "plugin-a"), draft);
        Finding second = Finding.from(observation("region-b", "region-y", "plugin-a"), draft);

        assertNotEquals(first.fingerprint(), second.fingerprint());
    }

    @Test
    void separatesOriginatingPluginsAndSchedulerTargets() {
        FindingDraft draft = draft();
        Finding first = Finding.from(observation("region-a", "region-x", "plugin-a"), draft);
        Finding second = Finding.from(observation("region-a", "region-x", "plugin-b"), draft);

        assertNotEquals(first.fingerprint(), second.fingerprint());
        assertEquals(FindingFingerprint.ALGORITHM_VERSION, "2");
    }

    private static FindingDraft draft() {
        return new FindingDraft("detector", "1", Severity.HIGH, Confidence.CONFIRMED,
                "summary", "explanation", RemediationCategory.REVIEW_THREAD_SAFETY, "");
    }

    private static Observation observation(String executionOwner, String targetOwner, String originatingPlugin) {
        return new Observation(
                UUID.randomUUID(), NOW, "responsible", originatingPlugin, OperationCategory.SCHEDULER_SUBMISSION,
                new ExecutionContext(ExecutionContextType.REGION, executionOwner, "region-thread", NOW),
                new OwnershipEvidence(new OwnershipKey(OwnershipType.REGION, targetOwner),
                        ResolutionSource.AUTHORITATIVE_API, Confidence.CONFIRMED, NOW, false),
                new ObservationOrigin("responsible", originatingPlugin, "scheduler", "region", "normal",
                        new CallSite("plugin.Plugin#submit", java.util.List.of("plugin.Plugin#submit"))),
                new CallSite("plugin.Plugin#run", java.util.List.of("plugin.Plugin#run")),
                Map.of("scheduler", "region", "targetKind", "entity")
        );
    }
}
