package com.foliarace.core.evidence;

import java.time.Instant;
import java.util.Objects;

public record OwnershipEvidence(
        OwnershipKey owner,
        ResolutionSource source,
        Confidence confidence,
        Instant observedAt,
        Boolean currentContextOwnsTarget
) {
    public OwnershipEvidence {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(confidence, "confidence");
        Objects.requireNonNull(observedAt, "observedAt");
    }

    public static OwnershipEvidence unknown(Instant observedAt) {
        return new OwnershipEvidence(
                new OwnershipKey(OwnershipType.UNKNOWN, ""),
                ResolutionSource.UNAVAILABLE,
                Confidence.INFORMATIONAL,
                observedAt,
                null
        );
    }

    public static OwnershipEvidence authoritativeCurrentContextCheck(boolean ownsTarget, Instant observedAt) {
        return new OwnershipEvidence(
                new OwnershipKey(OwnershipType.UNKNOWN, ""),
                ResolutionSource.AUTHORITATIVE_API,
                ownsTarget ? Confidence.CONFIRMED : Confidence.PROBABLE,
                observedAt,
                ownsTarget
        );
    }

    public boolean isKnown() {
        return owner.isKnown();
    }

    public boolean isAuthoritativeMismatch() {
        return source == ResolutionSource.AUTHORITATIVE_API && Boolean.FALSE.equals(currentContextOwnsTarget);
    }
}
