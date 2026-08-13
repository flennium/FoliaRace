package com.foliarace.plugin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyLoaderTest {
    @Test
    void loadsSchemaCorrectPoliciesAndRetainsExpiry() throws Exception {
        Path directory = Files.createTempDirectory("foliarace-policy");
        Path suppressions = directory.resolve("suppressions.yml");
        Files.writeString(suppressions, """
                schema-version: 1
                suppressions:
                  - detector-id: scheduler-misuse
                    plugin: example-plugin
                    call-site: com.example.ExamplePlugin#run
                    reason: approved migration
                    owner: team-example
                    created-at: 2026-01-01T00:00:00Z
                    expires-at: 2027-01-01T00:00:00Z
                """);

        var loaded = PolicyLoader.suppressions(suppressions.toFile());

        assertEquals(1, loaded.size());
        assertEquals("example-plugin", loaded.getFirst().plugin());
        assertTrue(loaded.getFirst().expired(java.time.Instant.parse("2027-01-01T00:00:00Z")));
    }

    @Test
    void rejectsMissingAndUnknownSuppressionKeys() throws Exception {
        Path directory = Files.createTempDirectory("foliarace-policy");
        Path missing = directory.resolve("missing.yml");
        Files.writeString(missing, """
                schema-version: 1
                suppressions:
                  - detector-id: scheduler-misuse
                    plugin: example-plugin
                    call-site: Example#run
                    owner: team-example
                    created-at: 2026-01-01T00:00:00Z
                    expires-at: 2027-01-01T00:00:00Z
                """);
        IllegalArgumentException missingError = assertThrows(IllegalArgumentException.class,
                () -> PolicyLoader.suppressions(missing.toFile()));
        assertTrue(missingError.getMessage().contains("reason"));

        Path unknown = directory.resolve("unknown.yml");
        Files.writeString(unknown, """
                schema-version: 1
                suppressions:
                  - detector-id: scheduler-misuse
                    plugin: example-plugin
                    call-site: Example#run
                    reason: approved migration
                    owner: team-example
                    created-at: 2026-01-01T00:00:00Z
                    expires-at: 2027-01-01T00:00:00Z
                    extra: rejected
                """);
        IllegalArgumentException unknownError = assertThrows(IllegalArgumentException.class,
                () -> PolicyLoader.suppressions(unknown.toFile()));
        assertTrue(unknownError.getMessage().contains("unknown key"));
    }

    @Test
    void loadsBaselineAndRejectsMalformedOrUnknownFields() throws Exception {
        Path directory = Files.createTempDirectory("foliarace-policy");
        Path valid = directory.resolve("baseline.json");
        Files.writeString(valid, "{\"schemaVersion\":\"1\",\"detectorVersions\":{\"scheduler-misuse\":\"1\"},\"fingerprints\":[\"fp-1\"]}");

        var baseline = PolicyLoader.baseline(valid.toFile());

        assertEquals("1", baseline.schemaVersion());
        assertEquals(java.util.Set.of("fp-1"), baseline.fingerprints());

        Path malformed = directory.resolve("malformed.json");
        Files.writeString(malformed, "{\"schemaVersion\":1,\"detectorVersions\":{},\"fingerprints\":[]}");
        IllegalArgumentException malformedError = assertThrows(IllegalArgumentException.class,
                () -> PolicyLoader.baseline(malformed.toFile()));
        assertTrue(malformedError.getMessage().contains("schemaVersion"));

        Path unknown = directory.resolve("unknown.json");
        Files.writeString(unknown, "{\"schemaVersion\":\"1\",\"detectorVersions\":{},\"fingerprints\":[],\"runtime\":\"26.2\"}");
        IllegalArgumentException unknownError = assertThrows(IllegalArgumentException.class,
                () -> PolicyLoader.baseline(unknown.toFile()));
        assertTrue(unknownError.getMessage().contains("unknown key"));
    }
}
