package com.foliarace.core.runtime;

import java.util.Set;

public record CompatibilityProfile(
        String id,
        String minecraftLine,
        String apiCoordinate,
        int minimumJava,
        Set<AdapterCapability> expectedCapabilities,
        String notes
) {
    public CompatibilityProfile {
        if (id == null || id.isBlank() || minecraftLine == null || minecraftLine.isBlank()
                || apiCoordinate == null || apiCoordinate.isBlank()) {
            throw new IllegalArgumentException("compatibility profile identity is required");
        }
        if (minimumJava < 17) {
            throw new IllegalArgumentException("Folia compatibility cannot target Java below 17");
        }
        expectedCapabilities = expectedCapabilities == null ? Set.of() : Set.copyOf(expectedCapabilities);
        notes = notes == null ? "" : notes.trim();
    }
}
