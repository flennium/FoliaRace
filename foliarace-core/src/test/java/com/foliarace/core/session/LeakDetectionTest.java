package com.foliarace.core.session;

import com.foliarace.core.config.FoliaRaceConfig;
import com.foliarace.core.finding.FindingAggregator;
import com.foliarace.core.pipeline.ObservationPipeline;
import com.foliarace.core.rule.DetectorCatalog;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LeakDetectionTest {
    @Test
    void stoppedDiagnosticObjectsAndWorkersAreCollectable() {
        List<WeakReference<?>> references = stoppedObjects();
        LeakCheckResult result = LeakDetector.awaitCollected(
                references, "foliarace-", Duration.ofSeconds(3));
        assertTrue(result.clean(), () -> "lifecycle leak detected: " + result);
    }

    private static List<WeakReference<?>> stoppedObjects() {
        ObservationPipeline activePipeline = new ObservationPipeline(
                32, DetectorCatalog.create(DetectorCatalog.ids()), new FindingAggregator());
        activePipeline.start();
        activePipeline.stop(Duration.ofSeconds(2));

        SessionManager activeSessions = new SessionManager();
        activeSessions.start("leak-gate", FoliaRaceConfig.defaults());
        activeSessions.stop();

        return List.of(new WeakReference<>(activePipeline), new WeakReference<>(activeSessions));
    }
}
