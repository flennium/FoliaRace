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
import java.util.Map;
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
    private static final List<String> SCENARIO_IDS = List.of("same-region-safe", "cross-region-unsafe", "async-state-access");
    private static final Map<String, ScenarioExpectation> SCENARIOS = Map.of(
            "same-region-safe", new ScenarioExpectation(null, "cross-region-ownership"),
            "cross-region-unsafe", new ScenarioExpectation("cross-region-ownership", null),
            "async-state-access", new ScenarioExpectation("async-server-state-access", null)
    );

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

        List<String> scenarios = scenario.equals("all") ? SCENARIO_IDS : List.of(scenario);
        if (scenarios.stream().anyMatch(value -> !SCENARIOS.containsKey(value))) {
            throw new IllegalArgumentException("unknown scenario '" + scenario + "'; expected one of " + SCENARIOS.keySet() + " or all");
        }
        boolean failed = false;
        for (String selected : scenarios) {
            try {
                runScenario(serverJar, foliaRaceJar, fixtureJar, selected, agentJar);
            } catch (Exception error) {
                failed = true;
                System.err.println("Folia integration scenario failed: " + selected + ": " + error.getMessage());
                if (!scenario.equals("all")) {
                    throw error;
                }
            }
        }
        if (failed) {
            throw new IllegalStateException("one or more Folia integration scenarios failed");
        }
    }

    private static void runScenario(Path serverJar, Path foliaRaceJar, Path fixtureJar,
                                    String scenario, Path agentJar) throws Exception {
        Path runDirectory = Files.createTempDirectory("foliarace-folia-" + scenario + "-");
        boolean passed = false;
        Process process = null;
        ExecutorService outputExecutor = Executors.newSingleThreadExecutor();
        Future<?> outputReader = null;
        try {
            seedMojangServerCache(runDirectory);
            Path plugins = Files.createDirectories(runDirectory.resolve("plugins"));
            Files.copy(foliaRaceJar, plugins.resolve("FoliaRace.jar"));
            Path fixtureDirectory = Files.createDirectories(plugins.resolve("FoliaRaceFixture"));
            Files.copy(fixtureJar, plugins.resolve("FoliaRaceFixture.jar"));
            Files.writeString(fixtureDirectory.resolve("config.yml"), "scenario: " + scenario + System.lineSeparator());
            Files.writeString(runDirectory.resolve("eula.txt"), "eula=true" + System.lineSeparator());
            Files.writeString(runDirectory.resolve("server.properties"),
                    "online-mode=false\nspawn-protection=0\nserver-ip=127.0.0.1\n" + System.lineSeparator());

            List<String> command = new java.util.ArrayList<>(List.of(
                    javaBinary(),
                    "-javaagent:" + agentJar,
                    "-jar",
                    serverJar.toString(),
                    "nogui"
            ));
            Path serverLog = runDirectory.resolve("server.log");
            process = new ProcessBuilder(command)
                    .directory(runDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();
            CountDownLatch readySignal = new CountDownLatch(1);
            CountDownLatch fixtureSignal = new CountDownLatch(1);
            Process runningProcess = process;
            outputReader = outputExecutor.submit(() -> streamOutput(runningProcess, readySignal, fixtureSignal, serverLog));
            try {
                if (!readySignal.await(Duration.ofMinutes(2).toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new IllegalStateException("Folia server did not become ready; see " + serverLog);
                }
                if (!fixtureSignal.await(Duration.ofSeconds(30).toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new IllegalStateException("fixture scenario did not complete; see " + serverLog);
                }
                sendCommand(process, "stop");
                if (!process.waitFor(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Folia server did not shut down cleanly");
                }
                if (process.exitValue() != 0) {
                    throw new IllegalStateException("Folia server exited with code " + process.exitValue() + "; see " + serverLog);
                }

                Path reportDirectory = plugins.resolve("FoliaRace").resolve("reports");
                List<Path> reports;
                try (Stream<Path> files = Files.exists(reportDirectory) ? Files.list(reportDirectory) : Stream.empty()) {
                    reports = files.filter(path -> path.toString().endsWith(".json"))
                            .sorted(Comparator.comparingLong(FoliaIntegrationHarness::lastModified)
                                    .thenComparing(Path::toString))
                            .toList();
                }
                if (reports.isEmpty()) {
                    throw new IllegalStateException("no FoliaRace report was produced; see " + runDirectory);
                }
                String report = Files.readString(reports.getLast());
                ScenarioExpectation expectation = SCENARIOS.get(scenario);
                if (expectation.requiredFinding() != null && !report.contains(expectation.requiredFinding())) {
                    throw new IllegalStateException("expected detector gap: " + expectation.requiredFinding() + " was not reported; artifacts preserved at " + runDirectory);
                }
                if (expectation.forbiddenFinding() != null && report.contains(expectation.forbiddenFinding())) {
                    throw new IllegalStateException("safe scenario produced " + expectation.forbiddenFinding() + "; artifacts preserved at " + runDirectory);
                }
                System.out.println("Folia integration scenario passed: " + scenario);
                passed = true;
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                if (outputReader != null) {
                    outputReader.cancel(true);
                }
            }
        } finally {
            outputExecutor.shutdownNow();
            if (passed && !Boolean.getBoolean("foliarace.preserveArtifacts")) {
                deleteTree(runDirectory);
            } else {
                System.err.println("Preserved Folia integration artifacts: " + runDirectory);
            }
        }
    }

    private static void streamOutput(Process process, CountDownLatch readySignal,
                                     CountDownLatch fixtureSignal, Path serverLog) {
        try (var output = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
             var log = Files.newBufferedWriter(serverLog, StandardCharsets.UTF_8)) {
            String line;
            while ((line = output.readLine()) != null) {
                System.out.println(line);
                log.write(line);
                log.newLine();
                log.flush();
                if (line.contains("Done (")) {
                    readySignal.countDown();
                }
                if (line.contains("fixture scenario=")) {
                    fixtureSignal.countDown();
                }
            }
        } catch (IOException error) {
            System.err.println("Could not read Folia output: " + error.getMessage());
        }
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException error) {
            return Long.MIN_VALUE;
        }
    }

    private record ScenarioExpectation(String requiredFinding, String forbiddenFinding) {
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
