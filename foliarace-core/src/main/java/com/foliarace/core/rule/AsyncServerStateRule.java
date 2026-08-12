package com.foliarace.core.rule;

import com.foliarace.core.context.ExecutionContextType;
import com.foliarace.core.evidence.Confidence;
import com.foliarace.core.finding.FindingDraft;
import com.foliarace.core.finding.RemediationCategory;
import com.foliarace.core.finding.Severity;
import com.foliarace.core.observation.OperationCategory;
import com.foliarace.core.observation.Observation;

import java.util.Set;
import java.util.Optional;

public final class AsyncServerStateRule implements DetectorRule {
    public static final String ID = "async-server-state-access";
    public static final String VERSION = "1";
    private static final Set<OperationCategory> RESTRICTED_OPERATIONS = Set.of(
            OperationCategory.LOCATION_ACCESS,
            OperationCategory.CHUNK_ACCESS,
            OperationCategory.BLOCK_ACCESS,
            OperationCategory.ENTITY_ACCESS,
            OperationCategory.PLAYER_ACCESS,
            OperationCategory.INVENTORY_ACCESS,
            OperationCategory.WORLD_GLOBAL_ACCESS,
            OperationCategory.SERVER_GLOBAL_ACCESS
    );

    @Override
    public String id() { return ID; }

    @Override
    public String version() { return VERSION; }

    @Override
    public Optional<FindingDraft> evaluate(Observation observation) {
        if (!RESTRICTED_OPERATIONS.contains(observation.operationCategory())
                || (observation.executionContext().type() != ExecutionContextType.ASYNC
                && observation.executionContext().type() != ExecutionContextType.PLUGIN_THREAD)) {
            return Optional.empty();
        }
        return Optional.of(new FindingDraft(
                ID,
                VERSION,
                Severity.HIGH,
                Confidence.PROBABLE,
                "Restricted server state was accessed from an asynchronous context",
                "The operation touched Folia-owned server state from an async or unmanaged plugin thread.",
                RemediationCategory.REVIEW_THREAD_SAFETY,
                "Execution context was classified from runtime evidence; verify the operation's documented thread-safety."
        ));
    }
}
