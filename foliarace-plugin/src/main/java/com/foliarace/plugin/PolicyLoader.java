package com.foliarace.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foliarace.core.finding.Baseline;
import com.foliarace.core.finding.Suppression;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;

final class PolicyLoader {
    private PolicyLoader() {
    }

    static List<Suppression> suppressions(File file) {
        if (!file.isFile()) {
            return List.of();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return yaml.getMapList("suppressions").stream().map(PolicyLoader::suppression).toList();
    }

    static Baseline baseline(File file) {
        if (!file.isFile()) {
            return null;
        }
        try {
            return new ObjectMapper().findAndRegisterModules().readValue(file, Baseline.class);
        } catch (Exception error) {
            throw new IllegalArgumentException("could not read baseline " + file.getName() + ": " + error.getMessage(), error);
        }
    }

    private static Suppression suppression(Map<?, ?> values) {
        return new Suppression(
                text(values, "detector-id"),
                text(values, "plugin"),
                text(values, "call-site"),
                text(values, "reason"),
                text(values, "owner"),
                instant(values, "created-at", Instant.EPOCH),
                instant(values, "expires-at", null)
        );
    }

    private static String text(Map<?, ?> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : value.toString();
    }

    private static Instant instant(Map<?, ?> values, String key, Instant fallback) {
        String value = text(values, key);
        return value.isBlank() ? fallback : Instant.parse(value);
    }
}
