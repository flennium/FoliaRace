package com.foliarace.core.finding;

import java.util.Set;

public record BaselineComparison(boolean comparable, Set<String> newFindings, Set<String> resolvedFindings, String reason) {
    public boolean hasNewFindings() {
        return !newFindings.isEmpty();
    }
}
