package com.foliarace.plugin;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginContractTest {
    @Test
    void pluginDescriptorDeclaresThePublicRuntimeContract() throws Exception {
        String descriptor = resource("/plugin.yml");

        assertTrue(descriptor.contains("api-version: '1.21'"));
        assertTrue(descriptor.contains("folia-supported: true"));
        assertTrue(descriptor.contains("/foliarace <status|start|stop|flush>"));
        assertTrue(descriptor.contains("default: op"));
    }

    @Test
    void fixtureDescriptorRequiresFoliaRaceFirst() throws Exception {
        String descriptor = Files.readString(Path.of("..", "foliarace-fixtures", "src", "main", "resources", "plugin.yml"));

        assertTrue(descriptor.contains("name: FoliaRaceFixture"));
        assertTrue(descriptor.contains("depend: [FoliaRace]"));
    }

    private static String resource(String name) throws Exception {
        try (InputStream stream = PluginContractTest.class.getResourceAsStream(name)) {
            if (stream == null) {
                throw new IllegalStateException("missing resource " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
