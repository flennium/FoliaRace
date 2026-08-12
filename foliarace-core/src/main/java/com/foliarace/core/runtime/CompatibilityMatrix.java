package com.foliarace.core.runtime;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Explicit runtime compatibility knowledge. Unknown versions never inherit
 * support merely because their version string looks similar.
 */
public final class CompatibilityMatrix {
    private static final Set<AdapterCapability> SCHEDULER_CAPABILITIES = Set.of(
            AdapterCapability.RUNTIME_IDENTITY,
            AdapterCapability.GLOBAL_SCHEDULER,
            AdapterCapability.REGION_SCHEDULER,
            AdapterCapability.ASYNC_SCHEDULER,
            AdapterCapability.ENTITY_SCHEDULER
    );

    private static final List<CompatibilityProfile> PROFILES = List.of(
            profile("folia-1.19.4", "1.19.4", "1.19.4-R0.1-SNAPSHOT", 17, "legacy API line"),
            profile("folia-1.20.1", "1.20.1", "1.20.1-R0.1-SNAPSHOT", 17, "legacy API line"),
            profile("folia-1.20.2", "1.20.2", "1.20.2-R0.1-SNAPSHOT", 17, "legacy API line"),
            profile("folia-1.20.4", "1.20.4", "1.20.4-R0.1-SNAPSHOT", 17, "legacy API line"),
            profile("folia-1.20.6", "1.20.6", "1.20.6-R0.1-SNAPSHOT", 21, "Java 21 API line"),
            profile("folia-1.21.4", "1.21.4", "1.21.4-R0.1-SNAPSHOT", 21, "legacy API coordinate"),
            profile("folia-1.21.5", "1.21.5", "1.21.5-R0.1-SNAPSHOT", 21, "legacy API coordinate"),
            profile("folia-1.21.6", "1.21.6", "1.21.6-R0.1-SNAPSHOT", 21, "legacy API coordinate"),
            profile("folia-1.21.8", "1.21.8", "1.21.8-R0.1-SNAPSHOT", 21, "legacy API coordinate"),
            profile("folia-1.21.11", "1.21.11", "1.21.11-R0.1-SNAPSHOT", 21, "final legacy coordinate"),
            profile("folia-26.1.2", "26.1", "26.1.2.build.8-stable", 25, "current versioned build coordinate"),
            profile("folia-26.2", "26.2", "26.2.build.4-beta", 25, "current beta build coordinate")
    );

    private CompatibilityMatrix() {
    }

    public static List<CompatibilityProfile> profiles() {
        return PROFILES;
    }

    public static CompatibilityResolution resolve(String runtimeVersion, Set<AdapterCapability> detectedCapabilities) {
        String detected = runtimeVersion == null ? "" : runtimeVersion.trim();
        Set<AdapterCapability> capabilities = detectedCapabilities == null ? Set.of() : Set.copyOf(detectedCapabilities);
        CompatibilityProfile profile = findProfile(detected);
        if (profile == null) {
            return new CompatibilityResolution(
                    detected,
                    CompatibilityStatus.UNSUPPORTED,
                    "unknown",
                    "unknown",
                    "Runtime is not in the verified Folia compatibility matrix.",
                    capabilities
            );
        }

        int currentJava = Runtime.version().feature();
        if (currentJava < profile.minimumJava()) {
            return new CompatibilityResolution(
                    detected,
                    CompatibilityStatus.DEGRADED,
                    profile.id(),
                    profile.minecraftLine(),
                    "The detected Java runtime is older than the profile minimum of Java " + profile.minimumJava() + ".",
                    capabilities
            );
        }

        EnumSet<AdapterCapability> missing = EnumSet.copyOf(profile.expectedCapabilities());
        missing.removeAll(capabilities);
        CompatibilityStatus status = missing.isEmpty() ? CompatibilityStatus.SUPPORTED : CompatibilityStatus.DEGRADED;
        String reason = missing.isEmpty()
                ? profile.notes()
                : "Runtime matched, but adapter capabilities are missing: " + missing;
        return new CompatibilityResolution(detected, status, profile.id(), profile.minecraftLine(), reason, capabilities);
    }

    private static CompatibilityProfile findProfile(String runtimeVersion) {
        String normalized = runtimeVersion.toLowerCase(Locale.ROOT);
        return PROFILES.stream()
                .filter(profile -> normalized.contains(profile.minecraftLine().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    private static CompatibilityProfile profile(String id, String minecraftLine, String apiCoordinate, int minimumJava, String notes) {
        return new CompatibilityProfile(id, minecraftLine, apiCoordinate, minimumJava, SCHEDULER_CAPABILITIES, notes);
    }
}
