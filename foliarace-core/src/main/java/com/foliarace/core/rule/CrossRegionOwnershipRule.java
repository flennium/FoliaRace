package com.foliarace.core.rule;

import com.foliarace.core.context.ExecutionContextType;
import com.foliarace.core.evidence.Confidence;
import com.foliarace.core.evidence.OwnershipType;
import com.foliarace.core.finding.FindingDraft;
import com.foliarace.core.finding.RemediationCategory;
import com.foliarace.core.finding.Severity;
import com.foliarace.core.observation.Observation;

import java.util.Optional;

/** Conservative first detector: it reports only authoritative, known region mismatches. */
public final class CrossRegionOwnershipRule implements DetectorRule {
    public static final String ID = "cross-region-ownership";
    public static final String VERSION = "1";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public Optional<FindingDraft> evaluate(Observation observation) {
        if (observation.targetOwnership().isAuthoritativeMismatch()) {
            return Optional.of(new FindingDraft(
                    ID,
                    VERSION,
                    Severity.HIGH,
                    Confidence.CONFIRMED,
                    "Region-owned state is not owned by the current region context",
                    "Folia's authoritative ownership check reported that the current execution context does not own the target. The target region identity was not exposed by the runtime API.",
                    RemediationCategory.USE_REGION_SCHEDULER,
                    "The runtime confirmed a mismatch but did not expose the target region identity."
            ));
        }

        if (observation.executionContext().type() != ExecutionContextType.REGION
                || !observation.executionContext().hasOwner()
                || !observation.targetOwnership().isKnown()
                || observation.targetOwnership().owner().type() != OwnershipType.REGION
                || observation.executionContext().ownerId().equals(observation.targetOwnership().owner().value())) {
            return Optional.empty();
        }

        return Optional.of(new FindingDraft(
                ID,
                VERSION,
                Severity.HIGH,
                Confidence.CONFIRMED,
                "Region-owned state was accessed from another region",
                "The operation executed in region '" + observation.executionContext().ownerId()
                        + "' while the target was owned by region '" + observation.targetOwnership().owner().value() + "'.",
                RemediationCategory.USE_REGION_SCHEDULER,
                "This detector requires authoritative region ownership evidence."
        ));
    }
}
