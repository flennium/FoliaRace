package com.foliarace.core.finding;

import java.time.Instant;

public record Suppression(
        String detectorId,
        String plugin,
        String callSite,
        String reason,
        String owner,
        Instant createdAt,
        Instant expiresAt
) {
    public Suppression {
        detectorId = normalize(detectorId);
        plugin = normalize(plugin);
        callSite = normalize(callSite);
        reason = normalize(reason);
        owner = normalize(owner);
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
    }

    public boolean expired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public boolean matches(Finding finding) {
        return (detectorId.isEmpty() || detectorId.equals(finding.detectorId()))
                && (plugin.isEmpty() || plugin.equals(finding.responsiblePlugin()))
                && (callSite.isEmpty() || callSite.equals(finding.callSite()));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
