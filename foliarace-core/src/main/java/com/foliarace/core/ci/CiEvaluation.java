package com.foliarace.core.ci;

import java.util.Set;

public record CiEvaluation(CiStatus status, int exitCode, Set<String> failingFingerprints, String reason) {
    public boolean successful() {
        return status == CiStatus.CLEAN;
    }
}
