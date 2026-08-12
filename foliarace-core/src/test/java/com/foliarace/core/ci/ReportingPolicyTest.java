package com.foliarace.core.ci;

import com.foliarace.core.finding.Baseline;
import com.foliarace.core.finding.BaselineComparator;
import com.foliarace.core.finding.Finding;
import com.foliarace.core.finding.FindingGroupSnapshot;
import com.foliarace.core.finding.Suppression;
import com.foliarace.core.finding.SuppressionMatcher;
import com.foliarace.core.report.JsonReportWriter;
import com.foliarace.core.report.MarkdownReportWriter;
import com.foliarace.core.report.ReportDocument;
import com.foliarace.core.runtime.CompatibilityStatus;
import com.foliarace.core.runtime.RuntimeDescriptor;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportingPolicyTest {
    private static final Instant NOW = Instant.parse("2026-08-12T00:00:00Z");

    @Test
    void suppressionsRemainVisibleAndExpire() {
        FindingGroupSnapshot group = group("fp-1", "detector", "plugin", "Example#run");
        Suppression suppression = new Suppression("detector", "plugin", "Example#run", "accepted test fixture", "owner", NOW, NOW.plusSeconds(60));
        List<FindingGroupSnapshot> suppressed = SuppressionMatcher.apply(List.of(group), List.of(suppression), NOW);

        assertTrue(suppressed.getFirst().suppressed());
        assertEquals("accepted test fixture", suppressed.getFirst().suppressionReason());
        assertFalse(SuppressionMatcher.apply(List.of(group), List.of(suppression), NOW.plusSeconds(60)).getFirst().suppressed());
    }

    @Test
    void baselineComparisonFindsOnlyNewFingerprints() {
        Baseline baseline = new Baseline("1", Map.of("detector", "1"), Set.of("old"));
        Baseline current = new Baseline("1", Map.of("detector", "1"), Set.of("old", "new"));

        var comparison = BaselineComparator.compare(baseline, current);

        assertTrue(comparison.comparable());
        assertEquals(Set.of("new"), comparison.newFindings());
    }

    @Test
    void incompatibleBaselineProducesIncompleteCoverage() {
        Baseline baseline = new Baseline("1", Map.of("detector", "1"), Set.of("old"));
        Baseline current = new Baseline("1", Map.of("detector", "2"), Set.of("old"));

        var comparison = BaselineComparator.compare(baseline, current);
        var evaluation = CiEvaluator.evaluate(List.of(), comparison, false, false);

        assertFalse(comparison.comparable());
        assertEquals(CiStatus.INCOMPLETE_COVERAGE, evaluation.status());
        assertEquals(2, evaluation.exitCode());
    }

    @Test
    void reportWritersProduceHumanAndMachineReadableArtifacts() throws Exception {
        FindingGroupSnapshot group = group("fp-1", "detector", "plugin", "Example#run");
        UUID session = UUID.randomUUID();
        ReportDocument report = new ReportDocument(
                "1", session, "test", NOW, "stopped",
                new RuntimeDescriptor("Folia", "26.2", "25", "test", "supported", CompatibilityStatus.SUPPORTED, "profile", "", Set.of()),
                List.of(group), Map.of()
        );
        var directory = Files.createTempDirectory("foliarace-report-policy");
        var json = directory.resolve("report.json");
        var markdown = directory.resolve("report.md");

        new JsonReportWriter().write(json, report);
        new MarkdownReportWriter().write(markdown, report);

        assertTrue(Files.readString(json).contains("schemaVersion"));
        assertTrue(Files.readString(markdown).contains("summary"));
    }

    private static FindingGroupSnapshot group(String fingerprint, String detector, String plugin, String callSite) {
        Finding finding = new Finding(
                new com.foliarace.core.finding.FindingFingerprint(fingerprint), detector, "1",
                com.foliarace.core.finding.Severity.HIGH, com.foliarace.core.evidence.Confidence.CONFIRMED,
                plugin, plugin, "BLOCK_ACCESS", "summary", "explanation", "REGION:region-a", "region-b", "REGION",
                callSite, "submission", NOW, com.foliarace.core.finding.RemediationCategory.USE_REGION_SCHEDULER, ""
        );
        return new FindingGroupSnapshot(finding, NOW, NOW, 1);
    }
}
