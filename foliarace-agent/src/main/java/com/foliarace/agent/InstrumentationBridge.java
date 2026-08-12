package com.foliarace.agent;

import java.util.concurrent.atomic.AtomicLong;

public final class InstrumentationBridge {
    private static volatile InstrumentationSink sink;
    private static final AtomicLong emittedEvents = new AtomicLong();
    private static final AtomicLong droppedEvents = new AtomicLong();
    private static final ThreadLocal<Boolean> recording = ThreadLocal.withInitial(() -> false);

    private InstrumentationBridge() {
    }

    public static void install(InstrumentationSink newSink) {
        sink = newSink;
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
}
