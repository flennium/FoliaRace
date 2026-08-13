package com.foliarace.core.runtime;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityMatrixTest {
    @Test
    void includesLegacyAndCurrentFoliaLines() {
        assertEquals(7, CompatibilityMatrix.profiles().size());
        assertTrue(CompatibilityMatrix.profiles().stream().noneMatch(profile -> profile.minecraftLine().equals("1.19.4")));
        assertTrue(CompatibilityMatrix.profiles().stream().anyMatch(profile -> profile.minecraftLine().equals("1.21.11")));
        assertTrue(CompatibilityMatrix.profiles().stream().anyMatch(profile -> profile.minecraftLine().equals("26.2")));
        assertTrue(CompatibilityMatrix.profiles().stream().allMatch(profile -> profile.minimumJava() == 25));
    }

    @Test
    void recognizesVerifiedLegacyRuntime() {
        CompatibilityResolution resolution = CompatibilityMatrix.resolve(
                "1.21.11-R0.1-SNAPSHOT",
                Set.of(
                        AdapterCapability.RUNTIME_IDENTITY,
                        AdapterCapability.GLOBAL_SCHEDULER,
                        AdapterCapability.REGION_SCHEDULER,
                        AdapterCapability.ASYNC_SCHEDULER,
                        AdapterCapability.ENTITY_SCHEDULER
                )
        );

        assertEquals(CompatibilityStatus.SUPPORTED, resolution.status());
        assertEquals("folia-1.21.11", resolution.profileId());
    }

    @Test
    void recognizesCurrentBuildFamily() {
        CompatibilityResolution resolution = CompatibilityMatrix.resolve(
                "26.2.0 (build 4)",
                Set.of(
                        AdapterCapability.RUNTIME_IDENTITY,
                        AdapterCapability.GLOBAL_SCHEDULER,
                        AdapterCapability.REGION_SCHEDULER,
                        AdapterCapability.ASYNC_SCHEDULER,
                        AdapterCapability.ENTITY_SCHEDULER
                )
        );

        assertEquals(CompatibilityStatus.SUPPORTED, resolution.status());
        assertEquals("folia-26.2", resolution.profileId());
    }

    @Test
    void degradesWhenARequiredCapabilityIsMissing() {
        CompatibilityResolution resolution = CompatibilityMatrix.resolve(
                "26.1.2 (build 8)",
                Set.of(AdapterCapability.RUNTIME_IDENTITY)
        );

        assertEquals(CompatibilityStatus.DEGRADED, resolution.status());
        assertTrue(resolution.reason().contains("missing"));
        assertTrue(resolution.isUsable());
    }

    @Test
    void rejectsUnknownVersionsWithoutGuessing() {
        CompatibilityResolution resolution = CompatibilityMatrix.resolve(
                "27.0.0 experimental",
                Set.of(AdapterCapability.RUNTIME_IDENTITY)
        );

        assertEquals(CompatibilityStatus.UNSUPPORTED, resolution.status());
        assertEquals("unknown", resolution.profileId());
        assertFalse(resolution.isUsable());
    }
}
