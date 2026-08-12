package com.foliarace.core.session;

import com.foliarace.core.config.FoliaRaceConfig;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class DiagnosticSession {
    private final UUID id;
    private final Instant startedAt;
    private final String label;
    private final FoliaRaceConfig config;
    private final AtomicReference<SessionState> state = new AtomicReference<>(SessionState.ACTIVE);
    private volatile Instant stoppedAt;

    DiagnosticSession(String label, FoliaRaceConfig config, Clock clock) {
        id = UUID.randomUUID();
        startedAt = Instant.now(clock);
        this.label = label == null ? "" : label.trim();
        this.config = config;
    }

    public UUID id() { return id; }
    public Instant startedAt() { return startedAt; }
    public String label() { return label; }
    public FoliaRaceConfig config() { return config; }
    public SessionState state() { return state.get(); }
    public Instant stoppedAt() { return stoppedAt; }

    void stop(Clock clock, SessionState finalState) {
        if (state.compareAndSet(SessionState.ACTIVE, finalState)) {
            stoppedAt = Instant.now(clock);
        }
    }
}
