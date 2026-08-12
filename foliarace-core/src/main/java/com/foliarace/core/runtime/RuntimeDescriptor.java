package com.foliarace.core.runtime;

public record RuntimeDescriptor(
        String platform,
        String runtimeVersion,
        String javaVersion,
        String adapterVersion,
        String coverageStatus,
        CompatibilityStatus compatibilityStatus,
        String compatibilityProfile,
        String compatibilityReason,
        java.util.Set<AdapterCapability> capabilities
) {
    public RuntimeDescriptor {
        compatibilityStatus = compatibilityStatus == null ? CompatibilityStatus.UNSUPPORTED : compatibilityStatus;
        compatibilityProfile = compatibilityProfile == null ? "unknown" : compatibilityProfile;
        compatibilityReason = compatibilityReason == null ? "" : compatibilityReason;
        capabilities = capabilities == null ? java.util.Set.of() : java.util.Set.copyOf(capabilities);
    }
}
