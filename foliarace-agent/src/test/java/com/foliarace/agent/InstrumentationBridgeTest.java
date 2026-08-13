package com.foliarace.agent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstrumentationBridgeTest {
    @Test
    void forwardsEventsAndExposesCounters() {
        AtomicInteger accepted = new AtomicInteger();
        InstrumentationSink sink = (owner, method, receiver, arguments) -> accepted.incrementAndGet();
        long emittedBefore = InstrumentationBridge.emittedEvents();

        InstrumentationBridge.install(sink);
        InstrumentationBridge.record("example.Owner", "read", new Object(), null);
        InstrumentationBridge.uninstall(sink);

        assertEquals(1, accepted.get());
        assertEquals(emittedBefore + 1, InstrumentationBridge.emittedEvents());
        assertFalse(InstrumentationBridge.installed());
    }

    @Test
    void countsSinkRejectionsAndTransformations() {
        long droppedBefore = InstrumentationBridge.droppedEvents();
        long failuresBefore = InstrumentationBridge.bridgeFailures();
        long transformedBefore = InstrumentationBridge.transformedTargets();
        long transformationFailuresBefore = InstrumentationBridge.transformationFailures();
        InstrumentationSink rejecting = (owner, method, receiver, arguments) -> {
            throw new IllegalStateException("rejected");
        };

        InstrumentationBridge.install(rejecting);
        InstrumentationBridge.record("example.Owner", "read", null, new Object[0]);
        InstrumentationBridge.uninstall(rejecting);
        InstrumentationBridge.recordTransformation();
        InstrumentationBridge.recordTransformationFailure("test failure");

        assertEquals(droppedBefore + 1, InstrumentationBridge.droppedEvents());
        assertEquals(failuresBefore + 1, InstrumentationBridge.bridgeFailures());
        assertEquals(transformedBefore + 1, InstrumentationBridge.transformedTargets());
        assertEquals(transformationFailuresBefore + 1, InstrumentationBridge.transformationFailures());
        assertTrue(InstrumentationBridge.failureReason().contains("test"));
    }
}
