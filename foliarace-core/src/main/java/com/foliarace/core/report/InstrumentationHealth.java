package com.foliarace.core.report;

import java.util.Map;

public record InstrumentationHealth(
        boolean installed,
        long transformedTargets,
        long emittedEvents,
        long droppedEvents,
        long bridgeFailures,
        long transformationFailures,
        String reason
) {
    public InstrumentationHealth {
        reason = reason == null || reason.isBlank() ? "available" : reason.trim();
        transformedTargets = Math.max(0, transformedTargets);
        emittedEvents = Math.max(0, emittedEvents);
        droppedEvents = Math.max(0, droppedEvents);
        bridgeFailures = Math.max(0, bridgeFailures);
        transformationFailures = Math.max(0, transformationFailures);
    }

    public static InstrumentationHealth unavailable(String reason) {
        return new InstrumentationHealth(false, 0, 0, 0, 0, 0,
                reason == null || reason.isBlank() ? "agent unavailable" : reason);
    }

    public Map<String, Object> asReportFields() {
        return Map.of(
                "instrumentationInstalled", installed,
                "instrumentationTransformedTargets", transformedTargets,
                "instrumentationEmittedEvents", emittedEvents,
                "instrumentationDroppedEvents", droppedEvents,
                "instrumentationBridgeFailures", bridgeFailures,
                "instrumentationTransformationFailures", transformationFailures,
                "instrumentationReason", reason
        );
    }
}
