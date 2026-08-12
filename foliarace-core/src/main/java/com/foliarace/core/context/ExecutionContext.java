package com.foliarace.core.context;

import java.time.Instant;

public record ExecutionContext(
        ExecutionContextType type,
        String ownerId,
        String threadName,
        Instant observedAt
) {
    public ExecutionContext {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        ownerId = ownerId == null ? "" : ownerId.trim();
        threadName = threadName == null ? "" : threadName.trim();
        if (observedAt == null) {
            throw new IllegalArgumentException("observedAt is required");
        }
    }

    public static ExecutionContext unknown(Instant observedAt, String threadName) {
        return new ExecutionContext(ExecutionContextType.UNKNOWN, "", threadName, observedAt);
    }

    public boolean hasOwner() {
        return !ownerId.isEmpty();
    }
}
