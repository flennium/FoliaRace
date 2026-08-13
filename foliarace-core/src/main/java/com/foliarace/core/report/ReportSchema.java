package com.foliarace.core.report;

public final class ReportSchema {
    public static final String CURRENT_VERSION = "1";

    private ReportSchema() {
    }

    public static boolean supports(String version) {
        return CURRENT_VERSION.equals(version);
    }
}
