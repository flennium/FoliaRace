package com.foliarace.core.finding;

import java.time.Instant;

public record FindingGroupSnapshot(
        Finding representative,
        Instant firstObservedAt,
        Instant lastObservedAt,
        long occurrenceCount
) {
}
