package com.foliarace.core.finding;

import java.util.Map;
import java.util.Set;

public final class BaselineComparator {
    private BaselineComparator() {
    }

    public static BaselineComparison compare(Baseline baseline, Baseline current) {
        if (baseline == null) {
            return new BaselineComparison(true, current.fingerprints(), Set.of(), "no baseline supplied");
        }
        if (!baseline.schemaVersion().equals(current.schemaVersion())) {
            return new BaselineComparison(false, Set.of(), Set.of(), "baseline schema versions differ");
        }
        for (Map.Entry<String, String> detector : baseline.detectorVersions().entrySet()) {
            if (!detector.getValue().equals(current.detectorVersions().get(detector.getKey()))) {
                return new BaselineComparison(false, Set.of(), Set.of(), "detector versions differ for " + detector.getKey());
            }
        }
        Set<String> newFindings = current.fingerprints().stream().filter(fingerprint -> !baseline.fingerprints().contains(fingerprint)).collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<String> resolvedFindings = baseline.fingerprints().stream().filter(fingerprint -> !current.fingerprints().contains(fingerprint)).collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new BaselineComparison(true, newFindings, resolvedFindings, "comparable");
    }
}
