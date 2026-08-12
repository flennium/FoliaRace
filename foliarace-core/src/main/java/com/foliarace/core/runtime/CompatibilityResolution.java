package com.foliarace.core.runtime;

import java.util.Set;

public record CompatibilityResolution(
        String detectedVersion,
        CompatibilityStatus status,
        String profileId,
        String minecraftLine,
        String reason,
        Set<AdapterCapability> capabilities
) {
    public CompatibilityResolution {
        detectedVersion = detectedVersion == null ? "unknown" : detectedVersion.trim();
        profileId = profileId == null ? "unknown" : profileId.trim();
        minecraftLine = minecraftLine == null ? "unknown" : minecraftLine.trim();
        reason = reason == null ? "" : reason.trim();
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }

    public boolean isUsable() {
        return status != CompatibilityStatus.UNSUPPORTED;
    }
}
