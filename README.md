# FoliaRace

FoliaRace is a development-time diagnostic aid for Folia plugin ownership and scheduler mistakes. It is not a formal proof that a plugin is thread-safe: a clean run means only that no enabled detector observed a violation during the exercised workload.

## Included implementation

The project now includes the complete diagnostic path: persistent configuration, runtime adapters with conservative ownership/context evidence, Folia fixtures and a real-server harness, optional automatic instrumentation, entity/async/scheduler detectors, JSON and Markdown reports, suppressions, baselines, CI evaluation, stress and lifecycle tests, compatibility verification, performance smoke tests, and reproducible release packaging.

The detailed product specification remains local at `docs/FoliaRace_README.md`. The `docs/` directory is intentionally ignored while the specification is being iterated, but the local file is preserved.

## Build

Use Java 25 for the default build:

```powershell
./gradlew test
./gradlew build
./gradlew compatibilityVerification
./gradlew performanceTest -PbenchmarkCount=25000
./gradlew release
```

`release` writes `build/release/FoliaRace-<version>.zip` and its SHA-256 checksum. The bundle contains the plugin, agent, fixture plugin, harness, compatibility matrix, configuration examples, and release documentation.

The plugin copies `config.yml` into its data folder on first start and validates it before enabling detectors. Invalid configuration disables FoliaRace rather than silently falling back to unsafe assumptions.

The plugin API target can be changed for compatibility compilation:

```powershell
./gradlew :foliarace-plugin:compileJava -PfoliaApiVersion=1.21.11-R0.1-SNAPSHOT
```

See [`compatibility/compatibility-matrix.md`](compatibility/compatibility-matrix.md) for the supported API coordinates and the distinction between resolver support and verified real-server coverage.

Automatic instrumentation is packaged separately so it can be enabled only in development or test runs:

```powershell
./gradlew :foliarace-agent:shadowJar
java -javaagent:foliarace-agent/build/libs/foliarace-agent-0.1.0-SNAPSHOT.jar -jar folia-server.jar nogui
```

The agent targets selected CraftBukkit world/entity access methods, forwards compact events through a guarded bridge, and leaves the operation result unchanged. If the agent is absent, FoliaRace continues in explicit-observation mode and reports the reduced coverage.

## Real-server integration

Supply a Folia server JAR explicitly; the task is skipped when no server is provided:

```powershell
./gradlew integrationTest -PfoliaServerJar=C:\path\to\folia-server.jar -PfixtureScenario=cross-region-unsafe
```

The harness creates an isolated temporary server directory, installs FoliaRace plus a fixture plugin, enables the agent, waits for the fixture scenario, and checks the generated report. Supported scenarios are `cross-region-unsafe`, `same-region-safe`, and `async-state-access`.

## Policy files and CI mode

Copy `config/suppressions.example.yml` and `config/baseline.example.json` into the plugin data directory as `suppressions.yml` and `baseline.json`. Set `ci-mode: true` to make new, unsuppressed findings fail the CI evaluation; baseline coverage is reported separately so stale baselines are visible.

The latest compatibility targets are listed in [`compatibility/compatibility-matrix.md`](compatibility/compatibility-matrix.md), and every default target is checked against `compatibility/verified-api-versions.txt`.
