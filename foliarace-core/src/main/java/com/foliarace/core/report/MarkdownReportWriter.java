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
        markdown.append("## Health\n\n");
        markdown.append("| Metric | Value |\n");
        markdown.append("| --- | --- |\n");
        report.health().forEach((key, value) -> markdown.append('|')
                .append(escape(key)).append('|')
                .append(escape(String.valueOf(value))).append("|\n"));
        markdown.append('\n');
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
            markdown.append('\n');
            for (FindingGroupSnapshot group : report.findings()) {
                var finding = group.representative();
                markdown.append("### ").append(escape(finding.detectorId())).append(" — ")
                        .append(escape(finding.summary())).append("\n\n")
                        .append("- Explanation: ").append(escape(finding.explanation())).append("\n")
                        .append("- Execution context: `").append(escape(finding.executionContext())).append("`\n")
                        .append("- Target ownership: `").append(escape(finding.targetOwnership())).append("` (`")
                        .append(escape(finding.targetType())).append("`)\n")
                        .append("- Call site: `").append(escape(finding.callSite())).append("`\n")
                        .append("- Submission site: `").append(escape(finding.submissionSite())).append("`\n")
                        .append("- Remediation: `").append(escape(finding.remediation().name())).append("`\n");
                if (!finding.limitation().isBlank()) {
                    markdown.append("- Limitation: ").append(escape(finding.limitation())).append("\n");
                }
                if (group.suppressed()) {
                    markdown.append("- Suppression reason: ").append(escape(group.suppressionReason())).append("\n");
                }
                markdown.append('\n');
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
