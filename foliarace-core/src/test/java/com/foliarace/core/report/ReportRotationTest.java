package com.foliarace.core.report;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportRotationTest {
    @Test
    void retainsEachFormatAndIgnoresStableLatestPointers() throws Exception {
        Path directory = Files.createTempDirectory("foliarace-rotation");
        Path firstJson = Files.writeString(directory.resolve("00000000-0000-0000-0000-000000000001.json"), "1");
        Path secondJson = Files.writeString(directory.resolve("00000000-0000-0000-0000-000000000002.json"), "2");
        Path firstMarkdown = Files.writeString(directory.resolve("00000000-0000-0000-0000-000000000001.md"), "1");
        Path secondMarkdown = Files.writeString(directory.resolve("00000000-0000-0000-0000-000000000002.md"), "2");
        Files.writeString(directory.resolve("latest.json"), "latest");
        Files.setLastModifiedTime(firstJson, java.nio.file.attribute.FileTime.fromMillis(1));
        Files.setLastModifiedTime(secondJson, java.nio.file.attribute.FileTime.fromMillis(2));
        Files.setLastModifiedTime(firstMarkdown, java.nio.file.attribute.FileTime.fromMillis(1));
        Files.setLastModifiedTime(secondMarkdown, java.nio.file.attribute.FileTime.fromMillis(2));

        ReportRotation.retain(directory, 1);

        assertEquals(1, Files.list(directory).filter(path -> path.toString().endsWith(".json")).count() - 1);
        assertTrue(Files.exists(directory.resolve("00000000-0000-0000-0000-000000000002.json")));
        assertTrue(Files.exists(directory.resolve("latest.json")));
        assertTrue(Files.exists(directory.resolve("00000000-0000-0000-0000-000000000002.md")));
    }
}
