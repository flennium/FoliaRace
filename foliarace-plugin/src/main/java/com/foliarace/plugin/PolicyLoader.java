package com.foliarace.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.foliarace.core.finding.Baseline;
import com.foliarace.core.finding.Suppression;
import com.foliarace.core.finding.FindingFingerprint;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PolicyLoader {
    private PolicyLoader() {
    }

    static List<Suppression> suppressions(File file) {
        if (!file.isFile()) {
            return List.of();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        requireKeys(yaml.getValues(false), Set.of("schema-version", "suppressions"), file.getName());
        requireSchemaVersion(yaml.get("schema-version"), file.getName() + ".schema-version");
        Object rawSuppressions = yaml.get("suppressions");
        if (!(rawSuppressions instanceof List<?> entries)) {
            throw invalid(file.getName() + ".suppressions", "must be a list");
        }
        return entries.stream().map(entry -> {
            if (!(entry instanceof Map<?, ?> values)) {
                throw invalid(file.getName() + ".suppressions[]", "must be an object");
            }
            requireKeys(values, Set.of("detector-id", "plugin", "call-site", "reason", "owner", "created-at", "expires-at"), file.getName() + ".suppressions[]");
            return suppression(values, file.getName() + ".suppressions[]");
        }).toList();
    }

    static Baseline baseline(File file) {
        if (!file.isFile()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            JsonNode root = mapper.readTree(file);
            if (root == null || !root.isObject()) {
                throw invalid(file.getName(), "must contain a JSON object");
            }
            Set<String> keys = new HashSet<>();
            root.fieldNames().forEachRemaining(keys::add);
            requireKeys(keys, Set.of("schemaVersion", "detectorVersions", "fingerprints", "fingerprintAlgorithm"), file.getName());
            if (!root.path("schemaVersion").isTextual() || !root.path("schemaVersion").asText().equals("1")) {
                throw invalid(file.getName() + ".schemaVersion", "must be the string '1'");
            }
            if (!root.path("detectorVersions").isObject()) {
                throw invalid(file.getName() + ".detectorVersions", "must be an object");
            }
            if (!root.path("fingerprints").isArray()) {
                throw invalid(file.getName() + ".fingerprints", "must be an array");
            }
            if (!root.path("fingerprintAlgorithm").isTextual()
                    || !FindingFingerprint.supportsAlgorithm(root.path("fingerprintAlgorithm").asText())) {
                throw invalid(file.getName() + ".fingerprintAlgorithm", "unsupported fingerprint algorithm");
            }
            return mapper.treeToValue(root, Baseline.class);
        } catch (Exception error) {
            if (error instanceof IllegalArgumentException argument) {
                throw argument;
            }
            throw new IllegalArgumentException("could not read baseline " + file.getName() + ": " + error.getMessage(), error);
        }
    }

    private static Suppression suppression(Map<?, ?> values, String path) {
        String detector = requiredText(values, "detector-id", path);
        String plugin = requiredText(values, "plugin", path);
        String callSite = requiredText(values, "call-site", path);
        String reason = requiredText(values, "reason", path);
        String owner = requiredText(values, "owner", path);
        Instant createdAt = instant(values, "created-at", path);
        Instant expiresAt = instant(values, "expires-at", path);
        if (!expiresAt.isAfter(createdAt)) {
            throw invalid(path + ".expires-at", "must be after created-at");
        }
        return new Suppression(
                detector, plugin, callSite, reason, owner, createdAt, expiresAt
        );
    }

    private static String requiredText(Map<?, ?> values, String key, String path) {
        String value = text(values, key);
        if (value.isBlank()) {
            throw invalid(path + "." + key, "is required");
        }
        return value;
    }

    private static String text(Map<?, ?> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : value.toString();
    }

    private static Instant instant(Map<?, ?> values, String key, String path) {
        Object raw = values.get(key);
        if (raw instanceof java.util.Date date) {
            return date.toInstant();
        }
        if (raw instanceof Instant instant) {
            return instant;
        }
        String value = text(values, key);
        if (value.isBlank()) {
            throw invalid(path + "." + key, "must be an ISO-8601 instant");
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException error) {
            throw invalid(path + "." + key, "must be an ISO-8601 instant");
        }
    }

    private static void requireSchemaVersion(Object value, String path) {
        if (!(value instanceof Number number) || number.intValue() != 1 || number.doubleValue() != 1) {
            throw invalid(path, "must be numeric version 1");
        }
    }

    private static void requireKeys(Map<?, ?> values, Set<String> expected, String path) {
        requireKeys(values.keySet(), expected, path);
    }

    private static void requireKeys(Set<?> actualValues, Set<String> expected, String path) {
        Set<String> actual = actualValues.stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet());
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(actual);
        if (!missing.isEmpty()) {
            throw invalid(path, "missing key(s): " + missing);
        }
        Set<String> unknown = new HashSet<>(actual);
        unknown.removeAll(expected);
        if (!unknown.isEmpty()) {
            throw invalid(path, "unknown key(s): " + unknown);
        }
    }

    private static IllegalArgumentException invalid(String path, String reason) {
        return new IllegalArgumentException(path + ": " + reason);
    }
}
