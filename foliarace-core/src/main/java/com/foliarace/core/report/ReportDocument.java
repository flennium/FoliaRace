package com.foliarace.core.report;

import com.foliarace.core.finding.FindingGroupSnapshot;
import com.foliarace.core.runtime.RuntimeDescriptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReportDocument(
        String schemaVersion,
        UUID sessionId,
        String sessionLabel,
        Instant generatedAt,
        String status,
        RuntimeDescriptor runtime,
        List<FindingGroupSnapshot> findings,
        Map<String, Object> health
) {
    public ReportDocument {
        if (schemaVersion == null || schemaVersion.isBlank() || !ReportSchema.supports(schemaVersion)) {
            throw new IllegalArgumentException("unsupported report schema version: " + schemaVersion);
        }
        if (sessionId == null || generatedAt == null || status == null || runtime == null) {
            throw new IllegalArgumentException("report identity and runtime fields are required");
        }
        findings = findings == null ? List.of() : List.copyOf(findings);
        health = health == null ? Map.of() : Map.copyOf(health);
    }
}
