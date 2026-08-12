package com.foliarace.core.runtime;

public record RuntimeDescriptor(
        String platform,
        String runtimeVersion,
        String javaVersion,
        String adapterVersion,
        String coverageStatus
) {
}
