package com.foliarace.core.config;

import com.foliarace.core.evidence.Confidence;
import com.foliarace.core.finding.Severity;

import java.util.Set;

public record FoliaRaceConfig(
        Set<String> enabledDetectors,
        OverheadMode overheadMode,
        long maxSessionDurationSeconds,
        Severity minimumSeverity,
        Confidence minimumConfidence,
        int observationQueueCapacity,
        double samplingRate,
        Set<OutputFormat> outputFormats,
        int reportRetentionCount,
        boolean productionMode,
        boolean productionAcknowledged,
        String suppressionFile,
        String baselineFile,
        boolean ciMode,
        boolean requireInstrumentation
) {
    public FoliaRaceConfig(
            Set<String> enabledDetectors,
            OverheadMode overheadMode,
            long maxSessionDurationSeconds,
            Severity minimumSeverity,
            Confidence minimumConfidence,
            int observationQueueCapacity,
            double samplingRate,
            Set<OutputFormat> outputFormats,
            boolean productionMode,
            boolean productionAcknowledged,
            String suppressionFile,
            String baselineFile,
            boolean ciMode
    ) {
        this(enabledDetectors, overheadMode, maxSessionDurationSeconds, minimumSeverity, minimumConfidence,
                observationQueueCapacity, samplingRate, outputFormats, 10, productionMode, productionAcknowledged,
                suppressionFile, baselineFile, ciMode, false);
    }

    public FoliaRaceConfig {
        enabledDetectors = enabledDetectors == null ? Set.of() : Set.copyOf(enabledDetectors);
        if (enabledDetectors.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("enabledDetectors cannot contain blank IDs");
        }
        if (overheadMode == null || minimumSeverity == null || minimumConfidence == null) {
            throw new IllegalArgumentException("detector configuration values are required");
        }
        if (maxSessionDurationSeconds < 1) {
            throw new IllegalArgumentException("maxSessionDurationSeconds must be positive");
        }
        if (observationQueueCapacity < 1) {
            throw new IllegalArgumentException("observationQueueCapacity must be positive");
        }
        if (samplingRate < 0 || samplingRate > 1 || Double.isNaN(samplingRate)) {
            throw new IllegalArgumentException("samplingRate must be between 0 and 1");
        }
        outputFormats = outputFormats == null || outputFormats.isEmpty() ? Set.of(OutputFormat.JSON) : Set.copyOf(outputFormats);
        if (reportRetentionCount < 1) {
            throw new IllegalArgumentException("reportRetentionCount must be positive");
        }
        suppressionFile = suppressionFile == null ? "" : suppressionFile.trim();
        baselineFile = baselineFile == null ? "" : baselineFile.trim();
        if (productionMode && !productionAcknowledged) {
            throw new IllegalArgumentException("productionMode requires productionAcknowledged");
        }
    }

    public static FoliaRaceConfig defaults() {
        return new FoliaRaceConfig(
                Set.of("cross-region-ownership", "cross-entity-ownership", "async-server-state-access", "scheduler-misuse"),
                OverheadMode.STANDARD,
                900,
                Severity.INFO,
                Confidence.INFORMATIONAL,
                8192,
                1.0,
                Set.of(OutputFormat.JSON),
                10,
                false,
                false,
                "suppressions.yml",
                "baseline.json",
                false,
                false
        );
    }
}
