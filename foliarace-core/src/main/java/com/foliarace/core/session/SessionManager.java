package com.foliarace.core.session;

import com.foliarace.core.config.FoliaRaceConfig;

import java.time.Clock;
import java.util.Optional;

public final class SessionManager {
    private final Clock clock;
    private DiagnosticSession current;

    public SessionManager() {
        this(Clock.systemUTC());
    }

    public SessionManager(Clock clock) {
        this.clock = clock;
    }

    public synchronized DiagnosticSession start(String label, FoliaRaceConfig config) {
        if (current != null && current.state() == SessionState.ACTIVE) {
            throw new IllegalStateException("a diagnostic session is already active");
        }
        current = new DiagnosticSession(label, config, clock);
        return current;
    }

    public synchronized Optional<DiagnosticSession> current() {
        return Optional.ofNullable(current).filter(session -> session.state() == SessionState.ACTIVE);
    }

    public synchronized Optional<DiagnosticSession> stop() {
        if (current == null) {
            return Optional.empty();
        }
        current.stop(clock, SessionState.STOPPED);
        return Optional.of(current);
    }
}
