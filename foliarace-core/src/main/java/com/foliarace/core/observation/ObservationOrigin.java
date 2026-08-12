package com.foliarace.core.observation;

public record ObservationOrigin(
        String responsiblePlugin,
        String originatingPlugin,
        String componentType,
        String componentName,
        String eventPriority,
        CallSite submissionSite
) {
    public ObservationOrigin {
        responsiblePlugin = normalize(responsiblePlugin);
        originatingPlugin = normalize(originatingPlugin);
        componentType = normalize(componentType);
        componentName = normalize(componentName);
        eventPriority = normalize(eventPriority);
        submissionSite = submissionSite == null ? CallSite.unknown() : submissionSite;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
