package com.foliarace.core.finding;

import com.foliarace.core.evidence.Confidence;

import java.util.List;

public final class FindingFilters {
    private FindingFilters() {
    }

    public static List<FindingGroupSnapshot> apply(
            List<FindingGroupSnapshot> groups,
            Severity minimumSeverity,
            Confidence minimumConfidence
    ) {
        if (groups == null) {
            return List.of();
        }
        if (minimumSeverity == null || minimumConfidence == null) {
            throw new IllegalArgumentException("finding filters are required");
        }
        return groups.stream()
                .filter(group -> group.representative().severity().ordinal() <= minimumSeverity.ordinal())
                .filter(group -> group.representative().confidence().ordinal() <= minimumConfidence.ordinal())
                .toList();
    }
}
