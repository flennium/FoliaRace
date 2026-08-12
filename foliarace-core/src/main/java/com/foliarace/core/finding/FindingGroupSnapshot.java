package com.foliarace.core.finding;

import java.time.Instant;

public record FindingGroupSnapshot(
        Finding representative,
        Instant firstObservedAt,
        Instant lastObservedAt,
        long occurrenceCount,
        boolean suppressed,
        String suppressionReason
) {
    public FindingGroupSnapshot(Finding representative, Instant firstObservedAt, Instant lastObservedAt, long occurrenceCount) {
        this(representative, firstObservedAt, lastObservedAt, occurrenceCount, false, "");
    }

    public FindingGroupSnapshot {
        suppressionReason = suppressionReason == null ? "" : suppressionReason;
    }
}
