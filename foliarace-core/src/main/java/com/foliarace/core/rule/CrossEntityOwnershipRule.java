package com.foliarace.core.rule;

import com.foliarace.core.evidence.Confidence;
import com.foliarace.core.finding.FindingDraft;
import com.foliarace.core.finding.RemediationCategory;
import com.foliarace.core.finding.Severity;
import com.foliarace.core.observation.OperationCategory;
import com.foliarace.core.observation.Observation;

import java.util.Set;
import java.util.Optional;

public final class CrossEntityOwnershipRule implements DetectorRule {
    public static final String ID = "cross-entity-ownership";
    public static final String VERSION = "1";
    private static final Set<OperationCategory> ENTITY_OPERATIONS = Set.of(
            OperationCategory.ENTITY_ACCESS,
            OperationCategory.PLAYER_ACCESS,
            OperationCategory.INVENTORY_ACCESS
    );

    @Override
    public String id() { return ID; }

    @Override
    public String version() { return VERSION; }

    @Override
    public Optional<FindingDraft> evaluate(Observation observation) {
        if (!ENTITY_OPERATIONS.contains(observation.operationCategory())
                || !observation.targetOwnership().isAuthoritativeMismatch()) {
            return Optional.empty();
        }
        return Optional.of(new FindingDraft(
                ID,
                VERSION,
                Severity.HIGH,
                Confidence.CONFIRMED,
                "Entity-owned state was accessed outside its current owner",
                "Folia's authoritative ownership check reported that the current execution context does not own the entity or player target.",
                RemediationCategory.USE_ENTITY_SCHEDULER,
                "The runtime confirmed a mismatch but did not expose the target entity scheduler identity."
        ));
    }
}
