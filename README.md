# FoliaRace

FoliaRace is a development-time diagnostic plugin for finding Folia ownership, context, and scheduler mistakes in Bukkit plugins. It records observations, evaluates conservative detectors, and writes reports that can be reviewed locally or enforced in CI.

FoliaRace is not a proof of thread safety. A clean report means that no enabled detector found a violation in the workload that was exercised.

## Requirements

- Java 25 for the default build and release artifacts
- A Folia server for real-server integration tests
- Gradle wrapper (`gradlew` or `gradlew.bat`)

## Build and verify

```powershell
./gradlew test build compatibilityVerification
./gradlew leakTest
./gradlew performanceTest -PbenchmarkCount=25000
./gradlew release
```

The release task writes `build/release/FoliaRace-<version>.zip` and a SHA-256 checksum. The archive contains the plugin, optional agent, fixture plugin, harness, configuration examples, compatibility records, and documentation.

## Install for a development server

1. Copy the FoliaRace plugin JAR from the release archive into the server's `plugins` directory.
2. Start the server once so the default `config.yml` is copied to the plugin data directory.
3. Adjust the enabled detectors and output settings in that file.
4. Exercise the plugin workload and inspect the generated report under the configured report directory.

Automatic instrumentation is optional. When enabled, the agent observes selected Bukkit/CraftBukkit accessors and forwards compact events without changing their return values:

```powershell
java -javaagent:foliarace-agent-0.1.0.jar -jar folia-server.jar nogui
```

Use explicit observations when the agent cannot cover a plugin-specific access path. See [TUTORIAL.md](TUTORIAL.md) for a complete first run.

## Real-server harness

The harness runs an isolated server with the fixture plugin and checks the expected result for one scenario:

```powershell
./gradlew integrationTest `
  -PfoliaServerJar=C:\servers\folia-1.21.11-14.jar `
  -PmojangServerJar=C:\servers\mojang-1.21.11.jar `
  -PmojangServerVersion=1.21.11 `
  -PfixtureScenario=cross-region-unsafe
```

Available scenarios are `cross-region-unsafe`, `same-region-safe`, and `async-state-access`. Use `-PmojangServerVersion` when testing a server line other than 1.21.11. Real-server evidence is recorded in [compatibility/real-server-coverage.md](compatibility/real-server-coverage.md); API and resolver coverage is listed separately in [compatibility/compatibility-matrix.md](compatibility/compatibility-matrix.md).

## CI policy

Set `ci-mode: true` in the plugin configuration. After the server run has produced a JSON report, enforce its recorded policy result with:

```powershell
./gradlew ciCheck -PciReport=C:\path\to\report.json
```

The task fails when the report is not CI-enabled or when the report contains unsuppressed findings. Suppressions and baselines are documented in [REPORTS.md](REPORTS.md).

Performance thresholds are enforced rather than merely printed:

```powershell
./gradlew performanceTest `
  -PbenchmarkCount=25000 `
  -PbenchmarkMinThroughput=100000 `
  -PbenchmarkMaxDropRate=0.90
```

## Compatibility

The plugin can be compiled against a selected API coordinate without changing source:

```powershell
./gradlew :foliarace-plugin:compileJava -PfoliaApiVersion=1.21.11-R0.1-SNAPSHOT
```

See the [compatibility matrix](compatibility/compatibility-matrix.md) before selecting a runtime. A resolver profile or successful API compilation does not replace real-server testing.

## Documentation

- [TUTORIAL.md](TUTORIAL.md) — first run and report walkthrough
- [DETECTORS.md](DETECTORS.md) — detector behavior and coverage
- [CONFIGURATION.md](CONFIGURATION.md) — configuration reference
- [REPORTS.md](REPORTS.md) — report schema, suppressions, baselines, and CI
- [compatibility/compatibility-matrix.md](compatibility/compatibility-matrix.md) — API targets
- [compatibility/real-server-coverage.md](compatibility/real-server-coverage.md) — executed server scenarios

The product specification remains in the local ignored file `docs/FoliaRace_README.md`.
