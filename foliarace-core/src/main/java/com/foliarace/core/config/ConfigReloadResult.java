package com.foliarace.core.config;

public record ConfigReloadResult(boolean accepted, FoliaRaceConfig activeConfig, String error) {
    public static ConfigReloadResult accepted(FoliaRaceConfig config) {
        return new ConfigReloadResult(true, config, "");
    }

    public static ConfigReloadResult rejected(FoliaRaceConfig activeConfig, RuntimeException error) {
        return new ConfigReloadResult(false, activeConfig, error.getMessage());
    }
}
