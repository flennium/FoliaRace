package com.foliarace.core.pipeline;

import com.foliarace.core.context.ExecutionContext;
import com.foliarace.core.context.ExecutionContextType;
import com.foliarace.core.evidence.OwnershipEvidence;
import com.foliarace.core.finding.FindingAggregator;
import com.foliarace.core.observation.CallSite;
import com.foliarace.core.observation.Observation;
import com.foliarace.core.observation.OperationCategory;
import com.foliarace.core.rule.DetectorRule;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationPipelineStressTest {
    @Test
    void concurrentOverflowDoesNotBlockOrLosePipelineHealthAccounting() throws Exception {
        FindingAggregator aggregator = new FindingAggregator();
        DetectorRule slowRule = new DetectorRule() {
            @Override
            public String id() {
                return "stress-delay";
            }

            @Override
            public String version() {
                return "1";
            }

            @Override
            public java.util.Optional<com.foliarace.core.finding.FindingDraft> evaluate(Observation observation) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                return java.util.Optional.empty();
            }
        };
        ObservationPipeline pipeline = new ObservationPipeline(64, List.of(slowRule), aggregator);
        pipeline.start();
        var executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var futures = java.util.stream.IntStream.range(0, 8).mapToObj(worker -> executor.submit(() -> {
                start.await();
                for (int index = 0; index < 500; index++) {
                    pipeline.submit(observation());
                }
                return null;
            })).toList();
            start.countDown();
            for (var future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            pipeline.stop(Duration.ofSeconds(5));
        }
        assertEquals(0, pipeline.pendingObservations());
        assertTrue(pipeline.droppedObservations() > 0);
    }

    private static Observation observation() {
        Instant now = Instant.now();
        return Observation.create(now, "stress", OperationCategory.BLOCK_ACCESS,
                new ExecutionContext(ExecutionContextType.REGION, "region-a", "stress", now),
                OwnershipEvidence.unknown(now), new CallSite("stress", List.of("stress")));
    }
}
