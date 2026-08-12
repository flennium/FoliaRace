package com.foliarace.core.report;

import com.foliarace.core.runtime.RuntimeDescriptor;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonReportWriterTest {
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
                new RuntimeDescriptor("Folia", "test", "25", "test", "limited"),
                java.util.List.of(),
                Map.of("droppedObservations", 0)
        );

        new JsonReportWriter().write(destination, report);
        String json = Files.readString(destination);

        assertTrue(json.contains("\"schemaVersion\" : \"1\""));
        assertTrue(json.contains(sessionId.toString()));
    }
}
