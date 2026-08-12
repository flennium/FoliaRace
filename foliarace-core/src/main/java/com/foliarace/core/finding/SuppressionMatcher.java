package com.foliarace.core.finding;

import java.time.Instant;
import java.util.List;

public final class SuppressionMatcher {
    private SuppressionMatcher() {
    }

    public static List<FindingGroupSnapshot> apply(List<FindingGroupSnapshot> groups, List<Suppression> suppressions, Instant now) {
        List<Suppression> active = suppressions == null ? List.of() : suppressions.stream().filter(suppression -> !suppression.expired(now)).toList();
        return groups.stream().map(group -> active.stream()
                        .filter(suppression -> suppression.matches(group.representative()))
                        .findFirst()
                        .map(suppression -> new FindingGroupSnapshot(
                                group.representative(), group.firstObservedAt(), group.lastObservedAt(), group.occurrenceCount(), true,
                                suppression.reason().isBlank() ? "suppressed by policy" : suppression.reason()))
                        .orElse(group))
                .toList();
    }
}
