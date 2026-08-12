package com.foliarace.core.pipeline;

import com.foliarace.core.context.ExecutionContext;
import com.foliarace.core.context.ExecutionContextType;
import com.foliarace.core.evidence.Confidence;
import com.foliarace.core.evidence.OwnershipEvidence;
import com.foliarace.core.evidence.OwnershipKey;
import com.foliarace.core.evidence.OwnershipType;
import com.foliarace.core.evidence.ResolutionSource;
import com.foliarace.core.finding.FindingAggregator;
import com.foliarace.core.observation.CallSite;
import com.foliarace.core.observation.Observation;
import com.foliarace.core.observation.OperationCategory;
import com.foliarace.core.rule.CrossRegionOwnershipRule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservationPipelineTest {
    @Test
    void groupsRepeatedFindingsWithoutBlockingTheCaller() {
        FindingAggregator aggregator = new FindingAggregator();
        ObservationPipeline pipeline = new ObservationPipeline(2, List.of(new CrossRegionOwnershipRule()), aggregator);
        Observation observation = observation();

        pipeline.process(observation);
        pipeline.process(observation);

        assertEquals(1, aggregator.groupCount());
        assertEquals(2, aggregator.snapshot().getFirst().occurrenceCount());
    }

    @Test
    void boundedQueueCountsOverflow() {
        FindingAggregator aggregator = new FindingAggregator();
        ObservationPipeline pipeline = new ObservationPipeline(1, List.of(), aggregator);

        assertEquals(0, pipeline.droppedObservations());
        assertEquals(false, pipeline.submit(observation()));
        assertEquals(1, pipeline.droppedObservations());
    }

    private static Observation observation() {
        Instant now = Instant.parse("2026-08-12T00:00:00Z");
        return Observation.create(
                now,
                "fixture-plugin",
                OperationCategory.BLOCK_ACCESS,
                new ExecutionContext(ExecutionContextType.REGION, "region-a", "region-thread", now),
                new OwnershipEvidence(
                        new OwnershipKey(OwnershipType.REGION, "region-b"),
                        ResolutionSource.AUTHORITATIVE_API,
                        Confidence.CONFIRMED,
                        now,
                        null
                ),
                new CallSite("fixture.Plugin#run", List.of("fixture.Plugin#run"))
        );
    }
}
