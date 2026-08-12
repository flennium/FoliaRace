package com.foliarace.core.rule;

import com.foliarace.core.evidence.Confidence;
import com.foliarace.core.finding.FindingDraft;
import com.foliarace.core.finding.RemediationCategory;
import com.foliarace.core.finding.Severity;
import com.foliarace.core.observation.Observation;

import java.util.Locale;
import java.util.Optional;

public final class SchedulerMisuseRule implements DetectorRule {
    public static final String ID = "scheduler-misuse";
    public static final String VERSION = "1";

    @Override
    public String id() { return ID; }

    @Override
    public String version() { return VERSION; }

    @Override
    public Optional<FindingDraft> evaluate(Observation observation) {
        if (observation.operationCategory() != com.foliarace.core.observation.OperationCategory.SCHEDULER_SUBMISSION) {
            return Optional.empty();
        }
        String scheduler = observation.metadata().getOrDefault("scheduler", "unknown").toLowerCase(Locale.ROOT);
        String targetKind = observation.metadata().getOrDefault("targetKind", "unknown").toLowerCase(Locale.ROOT);
        if (scheduler.equals("global") && (targetKind.equals("location") || targetKind.equals("block") || targetKind.equals("chunk"))) {
            return Optional.of(draft("Location-bound work was submitted to the global scheduler", "Use the region scheduler for work tied to a location, chunk, or block.", RemediationCategory.USE_REGION_SCHEDULER));
        }
        if (scheduler.equals("region") && (targetKind.equals("entity") || targetKind.equals("player") || targetKind.equals("inventory"))) {
            return Optional.of(draft("Entity-bound work was submitted to a region scheduler", "Use the entity scheduler so the callback follows the entity across region migration.", RemediationCategory.USE_ENTITY_SCHEDULER));
        }
        if (scheduler.equals("async") && !targetKind.equals("computation") && !targetKind.equals("none")) {
            return Optional.of(draft("Server-state work was submitted to the async scheduler", "Keep only pure computation, serialization, or networking on the async scheduler and transition back to the owning scheduler before server-state access.", RemediationCategory.REVIEW_THREAD_SAFETY));
        }
        return Optional.empty();
    }

    private static FindingDraft draft(String summary, String explanation, RemediationCategory remediation) {
        return new FindingDraft(ID, VERSION, Severity.MEDIUM, Confidence.PROBABLE, summary, explanation, remediation, "Scheduler target classification came from submission metadata.");
    }
}
