package com.foliarace.core.observation;

import com.foliarace.core.context.ExecutionContext;
import com.foliarace.core.evidence.OwnershipEvidence;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record Observation(
        UUID id,
        Instant observedAt,
        String responsiblePlugin,
        String originatingPlugin,
        OperationCategory operationCategory,
        ExecutionContext executionContext,
        OwnershipEvidence targetOwnership,
        ObservationOrigin origin,
        CallSite callSite,
        Map<String, String> metadata
) {
    public Observation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(observedAt, "observedAt");
        responsiblePlugin = normalize(responsiblePlugin);
        originatingPlugin = normalize(originatingPlugin);
        Objects.requireNonNull(operationCategory, "operationCategory");
        Objects.requireNonNull(executionContext, "executionContext");
        Objects.requireNonNull(targetOwnership, "targetOwnership");
        origin = origin == null ? new ObservationOrigin(responsiblePlugin, originatingPlugin, "", "", "", CallSite.unknown()) : origin;
        callSite = callSite == null ? CallSite.unknown() : callSite;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static Observation create(
            Instant observedAt,
            String responsiblePlugin,
            OperationCategory operationCategory,
            ExecutionContext executionContext,
            OwnershipEvidence targetOwnership,
            CallSite callSite
    ) {
        return new Observation(
                UUID.randomUUID(),
                observedAt,
                responsiblePlugin,
                "",
                operationCategory,
                executionContext,
                targetOwnership,
                null,
                callSite,
                Map.of()
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
