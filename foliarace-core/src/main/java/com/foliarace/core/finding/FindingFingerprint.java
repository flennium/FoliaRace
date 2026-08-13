package com.foliarace.core.finding;

import com.foliarace.core.observation.Observation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record FindingFingerprint(String value) {
    public static final String ALGORITHM_VERSION = "2";
    public FindingFingerprint {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("fingerprint is required");
        }
    }

    public static FindingFingerprint from(FindingDraft draft, Observation observation) {
        List<String> dimensions = List.of(
                ALGORITHM_VERSION,
                draft.detectorId(),
                observation.responsiblePlugin(),
                observation.originatingPlugin(),
                observation.origin().componentType(),
                observation.origin().componentName(),
                observation.callSite().primaryFrame(),
                observation.operationCategory().name(),
                observation.executionContext().type().name(),
                observation.executionContext().ownerId(),
                observation.targetOwnership().owner().type().name(),
                observation.targetOwnership().owner().value(),
                metadataDimension(observation.metadata(), "targetClass"),
                metadataDimension(observation.metadata(), "scheduler"),
                metadataDimension(observation.metadata(), "targetKind")
        );
        String raw = dimensions.stream().map(FindingFingerprint::field).collect(java.util.stream.Collectors.joining("|"));
        return new FindingFingerprint(sha256(raw));
    }

    public static boolean supportsAlgorithm(String version) {
        return ALGORITHM_VERSION.equals(version);
    }

    private static String metadataDimension(Map<String, String> metadata, String key) {
        return normalize(metadata.getOrDefault(key, ""));
    }

    private static String field(String value) {
        String normalized = normalize(value);
        return normalized.length() + ":" + normalized;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().replace('|', '_').replace('\n', '_').replace('\r', '_');
        return normalized.substring(0, Math.min(normalized.length(), 96)).toLowerCase(Locale.ROOT);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
