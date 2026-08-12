package com.foliarace.plugin;

import com.foliarace.core.config.FoliaRaceConfig;
import com.foliarace.core.config.OutputFormat;
import com.foliarace.core.config.OverheadMode;
import com.foliarace.core.evidence.Confidence;
import com.foliarace.core.finding.Severity;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class PluginConfigLoader {
    private PluginConfigLoader() {
    }

    static FoliaRaceConfig load(JavaPlugin plugin) {
        FileConfiguration file = plugin.getConfig();
        return new FoliaRaceConfig(
                Set.copyOf(file.getStringList("enabled-detectors")),
                enumValue(file.getString("overhead-mode"), OverheadMode.class, "overhead-mode"),
                file.getLong("max-session-duration-seconds"),
                enumValue(file.getString("minimum-severity"), Severity.class, "minimum-severity"),
                enumValue(file.getString("minimum-confidence"), Confidence.class, "minimum-confidence"),
                file.getInt("observation-queue-capacity"),
                file.getDouble("sampling-rate"),
                enumSet(file.getStringList("output-formats"), OutputFormat.class, "output-formats"),
                file.getBoolean("production-mode"),
                file.getBoolean("production-acknowledged")
        );
    }

    private static <E extends Enum<E>> E enumValue(String value, Class<E> type, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " must be configured");
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(key + " has unsupported value '" + value + "'", error);
        }
    }

    private static <E extends Enum<E>> Set<E> enumSet(java.util.List<String> values, Class<E> type, String key) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(key + " must contain at least one format");
        }
        return values.stream()
                .map(value -> enumValue(value, type, key))
                .collect(Collectors.toUnmodifiableSet());
    }
}
