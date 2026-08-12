package com.foliarace.core.finding;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class FindingAggregator {
    private final ConcurrentHashMap<FindingFingerprint, Group> groups = new ConcurrentHashMap<>();

    public void accept(Finding finding) {
        groups.computeIfAbsent(finding.fingerprint(), ignored -> new Group(finding)).accept(finding);
    }

    public int groupCount() {
        return groups.size();
    }

    public List<FindingGroupSnapshot> snapshot() {
        return groups.values().stream()
                .map(Group::snapshot)
                .sorted(Comparator.comparing(FindingGroupSnapshot::firstObservedAt))
                .toList();
    }

    private static final class Group {
        private final Finding representative;
        private final AtomicReference<Instant> firstObservedAt;
        private final AtomicReference<Instant> lastObservedAt;
        private final AtomicLong occurrenceCount = new AtomicLong();

        private Group(Finding initial) {
            representative = initial;
            firstObservedAt = new AtomicReference<>(initial.observedAt());
            lastObservedAt = new AtomicReference<>(initial.observedAt());
            occurrenceCount.set(0);
        }

        private void accept(Finding finding) {
            occurrenceCount.incrementAndGet();
            firstObservedAt.accumulateAndGet(finding.observedAt(), (current, candidate) -> candidate.isBefore(current) ? candidate : current);
            lastObservedAt.accumulateAndGet(finding.observedAt(), (current, candidate) -> candidate.isAfter(current) ? candidate : current);
        }

        private FindingGroupSnapshot snapshot() {
            return new FindingGroupSnapshot(representative, firstObservedAt.get(), lastObservedAt.get(), occurrenceCount.get());
        }
    }
}
