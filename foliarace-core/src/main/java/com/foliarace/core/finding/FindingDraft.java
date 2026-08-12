package com.foliarace.core.finding;

import com.foliarace.core.evidence.Confidence;

import java.util.Objects;

public record FindingDraft(
        String detectorId,
        String detectorVersion,
        Severity severity,
        Confidence confidence,
        String summary,
        String explanation,
        RemediationCategory remediation,
        String limitation
) {
    public FindingDraft {
        detectorId = requireText(detectorId, "detectorId");
        detectorVersion = requireText(detectorVersion, "detectorVersion");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(confidence, "confidence");
        summary = requireText(summary, "summary");
        explanation = requireText(explanation, "explanation");
        Objects.requireNonNull(remediation, "remediation");
        limitation = limitation == null ? "" : limitation.trim();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
