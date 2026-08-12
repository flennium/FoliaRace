package com.foliarace.core.config;

import java.util.concurrent.atomic.AtomicReference;

public final class ConfigManager {
    private final AtomicReference<FoliaRaceConfig> active;

    public ConfigManager(FoliaRaceConfig initial) {
        active = new AtomicReference<>(initial);
    }

    public FoliaRaceConfig current() {
        return active.get();
    }

    public ConfigReloadResult reload(java.util.function.Supplier<FoliaRaceConfig> candidateSupplier) {
        try {
            FoliaRaceConfig candidate = candidateSupplier.get();
            if (candidate == null) {
                throw new IllegalArgumentException("candidate configuration is null");
            }
            active.set(candidate);
            return ConfigReloadResult.accepted(candidate);
        } catch (RuntimeException error) {
            return ConfigReloadResult.rejected(active.get(), error);
        }
    }
}
