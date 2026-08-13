package com.foliarace.core.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;

public final class ReportRotation {
    private ReportRotation() {
    }

    public static void retain(Path directory, int retentionCount) throws IOException {
        if (retentionCount < 1 || !Files.isDirectory(directory)) {
            return;
        }
        for (String extension : new String[]{"json", "md"}) {
            try (Stream<Path> files = Files.list(directory)) {
                var reports = files
                        .filter(Files::isRegularFile)
                        .filter(path -> isSessionReport(path, extension))
                        .sorted(Comparator.comparingLong(ReportRotation::lastModified).reversed()
                                .thenComparing(Path::toString))
                        .toList();
                for (Path report : reports.stream().skip(retentionCount).toList()) {
                    Files.deleteIfExists(report);
                }
            }
        }
    }

    private static boolean isSessionReport(Path path, String extension) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\." + extension);
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException error) {
            return Long.MIN_VALUE;
        }
    }
}
