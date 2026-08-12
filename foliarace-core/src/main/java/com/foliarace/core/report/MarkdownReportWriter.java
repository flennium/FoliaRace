package com.foliarace.core.report;

import com.foliarace.core.finding.FindingGroupSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MarkdownReportWriter implements ReportWriter {
    @Override
    public void write(Path destination, ReportDocument report) throws IOException {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# FoliaRace report\n\n");
        markdown.append("- Schema: `").append(escape(report.schemaVersion())).append("`\n");
        markdown.append("- Session: `").append(report.sessionId()).append("`\n");
        markdown.append("- Status: `").append(escape(report.status())).append("`\n");
        markdown.append("- Runtime: `").append(escape(report.runtime().platform())).append(" ")
                .append(escape(report.runtime().runtimeVersion())).append("`\n\n");
        markdown.append("## Findings\n\n");
        if (report.findings().isEmpty()) {
            markdown.append("No findings were observed.\n");
        } else {
            markdown.append("| Severity | Confidence | Plugin | Detector | Count | Status | Summary |\n");
            markdown.append("| --- | --- | --- | --- | ---: | --- | --- |\n");
            for (FindingGroupSnapshot group : report.findings()) {
                var finding = group.representative();
                markdown.append('|').append(escape(finding.severity().name())).append('|')
                        .append(escape(finding.confidence().name())).append('|')
                        .append(escape(finding.responsiblePlugin())).append('|')
                        .append(escape(finding.detectorId())).append('|')
                        .append(group.occurrenceCount()).append('|')
                        .append(group.suppressed() ? "suppressed" : "active").append('|')
                        .append(escape(finding.summary())).append("|\n");
            }
        }
        Path parent = destination.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(destination, markdown);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\n", " ");
    }
}
