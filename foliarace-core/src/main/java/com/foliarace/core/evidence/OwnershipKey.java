package com.foliarace.core.evidence;

import java.util.Objects;

public record OwnershipKey(OwnershipType type, String value) {
    public OwnershipKey {
        Objects.requireNonNull(type, "type");
        value = value == null ? "" : value.trim();
    }

    public boolean isKnown() {
        return type != OwnershipType.UNKNOWN && !value.isEmpty();
    }
}
