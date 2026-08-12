package com.foliarace.core.observation;

import java.util.List;

public record CallSite(String primaryFrame, List<String> normalizedStack) {
    public CallSite {
        primaryFrame = primaryFrame == null ? "unknown" : primaryFrame.trim();
        normalizedStack = normalizedStack == null ? List.of() : List.copyOf(normalizedStack);
    }

    public static CallSite unknown() {
        return new CallSite("unknown", List.of());
    }
}
