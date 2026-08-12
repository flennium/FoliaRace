package com.foliarace.core.session;

import java.util.List;

public record LeakCheckResult(boolean clean, int liveReferences, List<String> liveThreads) {
    public LeakCheckResult {
        liveThreads = liveThreads == null ? List.of() : List.copyOf(liveThreads);
    }
}
