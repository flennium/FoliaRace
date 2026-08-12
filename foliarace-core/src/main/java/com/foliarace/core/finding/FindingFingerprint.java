package com.foliarace.core.finding;

import com.foliarace.core.observation.Observation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public record FindingFingerprint(String value) {
    public FindingFingerprint {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("fingerprint is required");
        }
    }

    public static FindingFingerprint from(FindingDraft draft, Observation observation) {
        String raw = String.join("|",
                draft.detectorId(),
                observation.responsiblePlugin(),
                observation.callSite().primaryFrame(),
                observation.operationCategory().name(),
                observation.executionContext().type().name(),
                observation.targetOwnership().owner().type().name()
        );
        return new FindingFingerprint(sha256(raw));
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
