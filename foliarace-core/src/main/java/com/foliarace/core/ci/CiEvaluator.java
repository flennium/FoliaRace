package com.foliarace.core.ci;

import com.foliarace.core.finding.BaselineComparison;
import com.foliarace.core.finding.FindingGroupSnapshot;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class CiEvaluator {
    private CiEvaluator() {
    }

    public static CiEvaluation evaluate(
            List<FindingGroupSnapshot> findings,
            BaselineComparison baseline,
            boolean incompleteCoverage,
            boolean instrumentationFailure
    ) {
        if (instrumentationFailure) {
            return new CiEvaluation(CiStatus.INSTRUMENTATION_FAILURE, 3, Set.of(), "instrumentation failed");
        }
        if (incompleteCoverage) {
            return new CiEvaluation(CiStatus.INCOMPLETE_COVERAGE, 2, Set.of(), "coverage is incomplete");
        }
        Set<String> failing = findings.stream()
                .filter(finding -> !finding.suppressed())
                .map(finding -> finding.representative().fingerprint().value())
                .collect(Collectors.toUnmodifiableSet());
        if (baseline != null && !baseline.comparable()) {
            return new CiEvaluation(CiStatus.INCOMPLETE_COVERAGE, 2, Set.of(), baseline.reason());
        }
        if (baseline != null) {
            failing = failing.stream().filter(fingerprint -> baseline.newFindings().contains(fingerprint)).collect(Collectors.toUnmodifiableSet());
        }
        if (!failing.isEmpty()) {
            return new CiEvaluation(CiStatus.FINDINGS_DETECTED, 1, failing, "unsuppressed findings detected");
        }
        return new CiEvaluation(CiStatus.CLEAN, 0, Set.of(), "no failing findings");
    }
}
