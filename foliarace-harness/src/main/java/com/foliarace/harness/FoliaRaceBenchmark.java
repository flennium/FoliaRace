package com.foliarace.harness;

import com.foliarace.core.context.ExecutionContext;
import com.foliarace.core.context.ExecutionContextType;
import com.foliarace.core.evidence.OwnershipEvidence;
import com.foliarace.core.finding.FindingAggregator;
import com.foliarace.core.observation.CallSite;
import com.foliarace.core.observation.Observation;
import com.foliarace.core.observation.OperationCategory;
import com.foliarace.core.pipeline.ObservationPipeline;
import com.foliarace.core.rule.DetectorCatalog;

import java.time.Duration;
import java.time.Instant;

/** Small reproducible smoke benchmark; use a real benchmark harness for release claims. */
public final class FoliaRaceBenchmark {
    private FoliaRaceBenchmark() {
    }

    public static void main(String[] args) {
        int count = args.length == 0 ? 100_000 : Integer.parseInt(args[0]);
        FindingAggregator findings = new FindingAggregator();
        try (ObservationPipeline pipeline = new ObservationPipeline(16_384, DetectorCatalog.create(DetectorCatalog.ids()), findings)) {
            pipeline.start();
            Observation observation = observation();
            long start = System.nanoTime();
            for (int index = 0; index < count; index++) {
                pipeline.submit(observation);
            }
            pipeline.stop(Duration.ofSeconds(10));
            long elapsed = System.nanoTime() - start;
            System.out.printf("observations=%d elapsedMillis=%.3f throughputPerSecond=%.2f dropped=%d groups=%d%n",
                    count, elapsed / 1_000_000.0, count / (elapsed / 1_000_000_000.0), pipeline.droppedObservations(), findings.groupCount());
        }
    }

    private static Observation observation() {
        Instant now = Instant.now();
        return Observation.create(now, "benchmark", OperationCategory.BLOCK_ACCESS,
                new ExecutionContext(ExecutionContextType.REGION, "region-a", "benchmark", now),
                OwnershipEvidence.unknown(now), new CallSite("benchmark", java.util.List.of("benchmark")));
    }
}
