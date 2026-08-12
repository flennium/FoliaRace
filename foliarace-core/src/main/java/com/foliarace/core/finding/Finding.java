package com.foliarace.core.finding;

import com.foliarace.core.observation.Observation;

import java.time.Instant;

public record Finding(
        FindingFingerprint fingerprint,
        String detectorId,
        String detectorVersion,
        Severity severity,
        com.foliarace.core.evidence.Confidence confidence,
        String responsiblePlugin,
        String originatingPlugin,
        String operationCategory,
        String summary,
        String explanation,
        String executionContext,
        String targetOwnership,
        String targetType,
        String callSite,
        String submissionSite,
        Instant observedAt,
        RemediationCategory remediation,
        String limitation
) {
    public static Finding from(Observation observation, FindingDraft draft) {
        return new Finding(
                FindingFingerprint.from(draft, observation),
                draft.detectorId(),
                draft.detectorVersion(),
                draft.severity(),
                draft.confidence(),
                observation.responsiblePlugin(),
                observation.originatingPlugin(),
                observation.operationCategory().name(),
                draft.summary(),
                draft.explanation(),
                observation.executionContext().type().name() + ":" + observation.executionContext().ownerId(),
                observation.targetOwnership().owner().value(),
                observation.targetOwnership().owner().type().name(),
                observation.callSite().primaryFrame(),
                observation.origin().submissionSite().primaryFrame(),
                observation.observedAt(),
                draft.remediation(),
                draft.limitation()
        );
    }
}
