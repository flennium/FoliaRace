package com.foliarace.core.finding;

import java.util.Map;
import java.util.Set;

public record Baseline(String schemaVersion, Map<String, String> detectorVersions, Set<String> fingerprints, String fingerprintAlgorithm) {
    public Baseline(String schemaVersion, Map<String, String> detectorVersions, Set<String> fingerprints) {
        this(schemaVersion, detectorVersions, fingerprints, FindingFingerprint.ALGORITHM_VERSION);
    }

    public Baseline {
        schemaVersion = schemaVersion == null ? "1" : schemaVersion;
        detectorVersions = detectorVersions == null ? Map.of() : Map.copyOf(detectorVersions);
        fingerprints = fingerprints == null ? Set.of() : Set.copyOf(fingerprints);
        fingerprintAlgorithm = fingerprintAlgorithm == null ? "" : fingerprintAlgorithm.trim();
    }

    public static Baseline from(ListFindingGroups groups) {
        return new Baseline("1", groups.detectorVersions(), groups.fingerprints(), FindingFingerprint.ALGORITHM_VERSION);
    }

    public record ListFindingGroups(Map<String, String> detectorVersions, Set<String> fingerprints) {
    }
}
