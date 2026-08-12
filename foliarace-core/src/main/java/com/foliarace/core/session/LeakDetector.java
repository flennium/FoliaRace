package com.foliarace.core.session;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

/** Lifecycle gate used by tests and release verification to detect retained sessions or workers. */
public final class LeakDetector {
    private LeakDetector() {
    }

    public static LeakCheckResult awaitCollected(
            Collection<? extends WeakReference<?>> references,
            String threadPrefix,
            Duration timeout
    ) {
        long deadline = System.nanoTime() + timeout.toNanos();
        List<? extends WeakReference<?>> probes = List.copyOf(references);
        List<byte[]> pressure = new ArrayList<>();
        while (System.nanoTime() < deadline) {
            System.gc();
            if (probes.stream().allMatch(reference -> reference.get() == null)) {
                break;
            }
            try {
                pressure.add(new byte[1024 * 1024]);
                if (pressure.size() == 16) {
                    pressure.clear();
                }
                Thread.sleep(25);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        List<String> liveThreads = new ArrayList<>();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.isAlive() && thread.getName().startsWith(threadPrefix)) {
                liveThreads.add(thread.getName());
            }
        }
        int liveReferences = (int) probes.stream().filter(reference -> reference.get() != null).count();
        return new LeakCheckResult(liveReferences == 0 && liveThreads.isEmpty(), liveReferences, liveThreads);
    }
}
