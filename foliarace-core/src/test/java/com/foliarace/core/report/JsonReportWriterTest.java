package com.foliarace.core.report;

import com.foliarace.core.runtime.RuntimeDescriptor;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonReportWriterTest {
    @Test
    void rejectsUnknownSchemaVersions() {
        assertThrows(IllegalArgumentException.class, () -> new ReportDocument(
                "2", UUID.randomUUID(), "test", Instant.now(), "stopped",
                new RuntimeDescriptor("Folia", "test", "25", "test", "supported",
                        com.foliarace.core.runtime.CompatibilityStatus.SUPPORTED, "profile", "", java.util.Set.of()),
                java.util.List.of(), Map.of()
        ));
    }

    @Test
    void writesVersionedMachineReadableReport() throws Exception {
        UUID sessionId = UUID.randomUUID();
        java.nio.file.Path destination = Files.createTempDirectory("foliarace-report-test").resolve("nested/report.json");
        ReportDocument report = new ReportDocument(
                "1",
                sessionId,
                "test",
                Instant.parse("2026-08-12T00:00:00Z"),
                "stopped",
                new RuntimeDescriptor(
                        "Folia", "test", "25", "test", "limited",
                        com.foliarace.core.runtime.CompatibilityStatus.DEGRADED,
                        "test-profile", "test reason", java.util.Set.of()
                ),
                java.util.List.of(),
                Map.of("droppedObservations", 0)
        );

        new JsonReportWriter().write(destination, report);
        String json = Files.readString(destination);

        assertTrue(json.contains("\"schemaVersion\" : \"1\""));
        assertTrue(json.contains(sessionId.toString()));
        var tree = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        assertEquals("1", tree.path("schemaVersion").asText());
        assertTrue(tree.has("runtime"));
        assertTrue(tree.has("findings"));
        assertTrue(tree.has("health"));
    }
}
