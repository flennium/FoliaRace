package com.foliarace.core.pipeline;

import com.foliarace.core.finding.Finding;
import com.foliarace.core.finding.FindingAggregator;
import com.foliarace.core.observation.Observation;
import com.foliarace.core.rule.DetectorRule;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class ObservationPipeline implements AutoCloseable {
    private final ArrayBlockingQueue<Observation> queue;
    private final List<DetectorRule> rules;
    private final FindingAggregator aggregator;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicLong droppedObservations = new AtomicLong();
    private final AtomicLong ruleFailures = new AtomicLong();
    private Thread worker;

    public ObservationPipeline(int capacity, List<DetectorRule> rules, FindingAggregator aggregator) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.rules = List.copyOf(rules);
        this.aggregator = aggregator;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        worker = new Thread(this::runWorker, "foliarace-observation-pipeline");
        worker.setDaemon(true);
        worker.start();
    }

    public boolean submit(Observation observation) {
        if (!running.get() || !queue.offer(observation)) {
            droppedObservations.incrementAndGet();
            return false;
        }
        return true;
    }

    public void process(Observation observation) {
        for (DetectorRule rule : rules) {
            try {
                rule.evaluate(observation).map(draft -> Finding.from(observation, draft)).ifPresent(aggregator::accept);
            } catch (RuntimeException ignored) {
                ruleFailures.incrementAndGet();
            }
        }
    }

    public void stop(Duration timeout) {
        running.set(false);
        Thread currentWorker = worker;
        if (currentWorker == null) {
            return;
        }
        try {
            currentWorker.join(timeout.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        if (currentWorker.isAlive()) {
            currentWorker.interrupt();
        }
        worker = null;
    }

    public long droppedObservations() {
        return droppedObservations.get();
    }

    public long ruleFailures() {
        return ruleFailures.get();
    }

    public int pendingObservations() {
        return queue.size();
    }

    @Override
    public void close() {
        stop(Duration.ofSeconds(2));
    }

    private void runWorker() {
        while (running.get() || !queue.isEmpty()) {
            try {
                Observation observation = queue.poll(50, TimeUnit.MILLISECONDS);
                if (observation != null) {
                    process(observation);
                }
            } catch (InterruptedException interrupted) {
                if (!running.get()) {
                    break;
                }
                Thread.currentThread().interrupt();
            }
        }
    }
}
