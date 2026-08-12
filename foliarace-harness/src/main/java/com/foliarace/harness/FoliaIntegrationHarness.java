package com.foliarace.harness;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

/**
 * Launches a real Folia server for a deterministic fixture scenario.
 *
 * Required arguments: server.jar foliarace.jar fixture.jar scenario agent.jar
 */
public final class FoliaIntegrationHarness {
    private FoliaIntegrationHarness() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException("usage: FoliaIntegrationHarness <server.jar> <foliarace.jar> <fixture.jar> <scenario> <agent.jar>");
        }

        Path serverJar = Path.of(args[0]).toAbsolutePath();
        Path foliaRaceJar = Path.of(args[1]).toAbsolutePath();
        Path fixtureJar = Path.of(args[2]).toAbsolutePath();
        String scenario = args[3].toLowerCase(Locale.ROOT);
        Path agentJar = Path.of(args[4]).toAbsolutePath();
        requireFile(serverJar);
        requireFile(foliaRaceJar);
        requireFile(fixtureJar);
        requireFile(agentJar);

        Path runDirectory = Files.createTempDirectory("foliarace-folia-");
        try {
            seedMojangServerCache(runDirectory);
            Path plugins = Files.createDirectories(runDirectory.resolve("plugins"));
            Files.copy(foliaRaceJar, plugins.resolve("FoliaRace.jar"));
            Path fixtureDirectory = Files.createDirectories(plugins.resolve("FoliaRaceFixture"));
            Files.copy(fixtureJar, plugins.resolve("FoliaRaceFixture.jar"));
            Files.writeString(fixtureDirectory.resolve("config.yml"), "scenario: " + scenario + System.lineSeparator());
            Files.writeString(runDirectory.resolve("eula.txt"), "eula=true" + System.lineSeparator());
            Files.writeString(runDirectory.resolve("server.properties"),
                    "online-mode=false\nspawn-protection=0\n" + System.lineSeparator());

            List<String> command = new java.util.ArrayList<>(List.of(
                    javaBinary(),
                    "-javaagent:" + agentJar,
                    "-jar",
                    serverJar.toString(),
                    "nogui"
            ));
            Process process = new ProcessBuilder(command)
                    .directory(runDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();
            ExecutorService outputExecutor = Executors.newSingleThreadExecutor();
            CountDownLatch readySignal = new CountDownLatch(1);
            Future<?> outputReader = outputExecutor.submit(() -> streamOutput(process, readySignal));
            try {
                if (!readySignal.await(Duration.ofMinutes(2).toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new IllegalStateException("Folia server did not become ready");
                }

                Thread.sleep(Duration.ofSeconds(8).toMillis());
                sendCommand(process, "stop");
                if (!process.waitFor(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Folia server did not shut down cleanly");
                }

                Path reportDirectory = plugins.resolve("FoliaRace").resolve("reports");
                List<Path> reports;
                try (Stream<Path> files = Files.exists(reportDirectory) ? Files.list(reportDirectory) : Stream.empty()) {
                    reports = files.filter(path -> path.toString().endsWith(".json")).toList();
                }
                if (reports.isEmpty()) {
                    throw new IllegalStateException("No FoliaRace report was produced");
                }
                String report = Files.readString(reports.getFirst());
                if (scenario.equals("cross-region-unsafe") && !report.contains("cross-region-ownership")) {
                    throw new IllegalStateException("Unsafe fixture produced no cross-region finding");
                }
                if (scenario.equals("same-region-safe") && report.contains("cross-region-ownership")) {
                    throw new IllegalStateException("Safe fixture produced a cross-region finding");
                }
                if (scenario.equals("async-state-access") && !report.contains("async-server-state-access")) {
                    throw new IllegalStateException("Async fixture produced no async-state finding");
                }
                System.out.println("Folia integration scenario passed: " + scenario);
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                outputReader.cancel(true);
                outputExecutor.shutdownNow();
            }
        } finally {
            deleteTree(runDirectory);
        }
    }

    private static void streamOutput(Process process, CountDownLatch readySignal) {
        try (var output = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = output.readLine()) != null) {
                System.out.println(line);
                if (line.contains("Done (")) {
                    readySignal.countDown();
                }
            }
        } catch (IOException error) {
            System.err.println("Could not read Folia output: " + error.getMessage());
        }
    }

    private static void sendCommand(Process process, String command) throws IOException {
        try (PrintWriter input = new PrintWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8), true)) {
            input.println(command);
        }
    }

    private static String javaBinary() {
        String javaHome = System.getProperty("java.home");
        return Path.of(javaHome, "bin", isWindows() ? "java.exe" : "java").toString();
    }

    private static void seedMojangServerCache(Path runDirectory) throws IOException {
        String source = System.getProperty("foliarace.mojangJar", "");
        String version = System.getProperty("foliarace.mojangVersion", "1.21.11");
        if (source.isBlank()) {
            return;
        }
        Path sourcePath = Path.of(source).toAbsolutePath();
        requireFile(sourcePath);
        Path cache = Files.createDirectories(runDirectory.resolve("cache"));
        Files.copy(sourcePath, cache.resolve("mojang_" + version + ".jar"));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static void requireFile(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("missing file: " + path);
        }
    }

    private static void deleteTree(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException error) {
                    System.err.println("Could not delete harness artifact " + path + ": " + error.getMessage());
                }
            });
        }
    }
}
