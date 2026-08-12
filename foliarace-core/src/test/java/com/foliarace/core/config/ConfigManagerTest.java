package com.foliarace.core.config;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {
    @Test
    void invalidReloadLeavesLastValidConfigurationActive() {
        FoliaRaceConfig initial = FoliaRaceConfig.defaults();
        ConfigManager manager = new ConfigManager(initial);

        ConfigReloadResult result = manager.reload(() -> new FoliaRaceConfig(
                Set.of("cross-region-ownership"),
                OverheadMode.STANDARD,
                900,
                initial.minimumSeverity(),
                initial.minimumConfidence(),
                0,
                1.0,
                Set.of(OutputFormat.JSON),
                false,
                false
        ));

        assertFalse(result.accepted());
        assertEquals(initial, manager.current());
        assertTrue(result.error().contains("observationQueueCapacity"));
    }

    @Test
    void productionModeRequiresExplicitAcknowledgement() {
        FoliaRaceConfig defaults = FoliaRaceConfig.defaults();

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new FoliaRaceConfig(
                defaults.enabledDetectors(),
                defaults.overheadMode(),
                defaults.maxSessionDurationSeconds(),
                defaults.minimumSeverity(),
                defaults.minimumConfidence(),
                defaults.observationQueueCapacity(),
                defaults.samplingRate(),
                defaults.outputFormats(),
                true,
                false
        ));
    }
}
