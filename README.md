# FoliaRace

FoliaRace is a development-time diagnostic aid for Folia plugin ownership and scheduler mistakes. It is not a formal proof that a plugin is thread-safe: a clean run means only that no enabled detector observed a violation during the exercised workload.

## Current state

The repository currently contains the first foundation milestone:

- a Java 25 Gradle multi-module build;
- a platform-neutral core for observations, ownership evidence, findings, sessions, bounded processing, and JSON reports;
- a conservative cross-region detector;
- a Folia plugin entry point with runtime capability detection;
- an explicit compatibility resolver covering Folia API lines from 1.19.4 through 1.21.11 and current 26.x builds.

The detailed product specification remains local at `docs/FoliaRace_README.md`. The `docs/` directory is intentionally ignored while the specification is being iterated.

## Build

Use Java 25 for the default build:

```powershell
./gradlew test
./gradlew build
```

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
