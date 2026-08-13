package com.foliarace.core.session;

import com.foliarace.core.config.FoliaRaceConfig;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionLifecycleTest {
    @Test
    void sessionStopsExactlyOnceAndDoesNotRemainActive() {
        SessionManager manager = new SessionManager(Clock.fixed(Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC));
        DiagnosticSession session = manager.start("lifecycle", FoliaRaceConfig.defaults());

        assertEquals(SessionState.ACTIVE, session.state());
        assertTrue(manager.stop().isPresent());
        assertEquals(SessionState.STOPPED, session.state());
        assertTrue(manager.current().isEmpty());
        assertTrue(manager.stop().isEmpty());
        assertEquals(SessionState.STOPPED, session.state());
    }
}
