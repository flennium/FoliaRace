package com.foliarace.harness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foliarace.core.report.ReportDocument;

import java.nio.file.Path;

/** Evaluates a completed FoliaRace report as a build step. */
public final class FoliaRaceCi {
    private FoliaRaceCi() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: FoliaRaceCi <report.json>");
        }
        ReportDocument report = new ObjectMapper().findAndRegisterModules()
                .readValue(Path.of(args[0]).toFile(), ReportDocument.class);
        Object enabled = report.health().get("ciMode");
        if (!Boolean.TRUE.equals(enabled)) {
            throw new IllegalStateException("report was not generated with ci-mode: true");
        }
        int exitCode = ((Number) report.health().getOrDefault("ciExitCode", 2)).intValue();
        String status = String.valueOf(report.health().getOrDefault("ciStatus", "UNKNOWN"));
        System.out.println("FoliaRace CI status=" + status + " exitCode=" + exitCode);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
