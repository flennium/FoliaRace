package com.foliarace.agent;

import java.util.concurrent.atomic.AtomicLong;

public final class InstrumentationBridge {
    private static volatile InstrumentationSink sink;
    private static final AtomicLong emittedEvents = new AtomicLong();
    private static final AtomicLong droppedEvents = new AtomicLong();
    private static final AtomicLong bridgeFailures = new AtomicLong();
    private static final AtomicLong transformedTargets = new AtomicLong();
    private static final AtomicLong transformationFailures = new AtomicLong();
    private static volatile String failureReason = "available";
    private static final ThreadLocal<Boolean> recording = ThreadLocal.withInitial(() -> false);

    private InstrumentationBridge() {
    }

    public static void install(InstrumentationSink newSink) {
        sink = newSink;
        if (failureReason == null || failureReason.equals("available")) {
            failureReason = "available";
        }
    }

    public static void uninstall(InstrumentationSink oldSink) {
        if (sink == oldSink) {
            sink = null;
        }
    }

    public static void record(String ownerType, String methodName, Object receiver, Object[] arguments) {
        InstrumentationSink current = sink;
        if (current == null) {
            droppedEvents.incrementAndGet();
            return;
        }
        if (recording.get()) {
            return;
        }
        emittedEvents.incrementAndGet();
        recording.set(true);
        try {
            current.accept(ownerType, methodName, receiver, arguments == null ? new Object[0] : arguments);
        } catch (RuntimeException ignored) {
            droppedEvents.incrementAndGet();
            bridgeFailures.incrementAndGet();
            failureReason = "instrumentation sink rejected an event";
        } finally {
            recording.set(false);
        }
    }

    public static long emittedEvents() {
        return emittedEvents.get();
    }

    public static long droppedEvents() {
        return droppedEvents.get();
    }

    public static long bridgeFailures() {
        return bridgeFailures.get();
    }

    public static long transformedTargets() {
        return transformedTargets.get();
    }

    public static long transformationFailures() {
        return transformationFailures.get();
    }

    public static boolean installed() {
        return sink != null;
    }

    public static String failureReason() {
        return failureReason;
    }

    public static void recordTransformation() {
        transformedTargets.incrementAndGet();
    }

    public static void recordTransformationFailure(String reason) {
        transformationFailures.incrementAndGet();
        failureReason = reason == null || reason.isBlank() ? "instrumentation transformation failed" : reason;
    }

    public static void recordBridgeFailure(String reason) {
        bridgeFailures.incrementAndGet();
        failureReason = reason == null || reason.isBlank() ? "instrumentation bridge failed" : reason;
    }
}
