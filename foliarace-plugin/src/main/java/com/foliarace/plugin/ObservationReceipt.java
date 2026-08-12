package com.foliarace.plugin;

public record ObservationReceipt(
        boolean accepted,
        boolean ownershipCheckAvailable,
        Boolean ownedByCurrentContext,
        String reason
) {
    public ObservationReceipt {
        reason = reason == null ? "" : reason;
    }

    static ObservationReceipt unavailable(String reason) {
        return new ObservationReceipt(false, false, null, reason);
    }
}
