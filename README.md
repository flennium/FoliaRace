# FoliaRace

![Build](https://github.com/flennium/FoliaRace/actions/workflows/ci.yml/badge.svg)
![Release](https://img.shields.io/github/v/release/flennium/FoliaRace?display_name=tag)

FoliaRace is a development-time diagnostic plugin for Folia and Bukkit plugins. It records ownership and execution-context observations, evaluates conservative detectors, and produces reports that can be reviewed locally or enforced in CI.

> FoliaRace is not a proof of thread safety. A clean report only means that no enabled detector found a violation in the workload that was exercised.

## Start here

| Need | Read |
| --- | --- |
| First run | [docs/TUTORIAL.md](docs/TUTORIAL.md) |
| Configure the plugin | [docs/CONFIGURATION.md](docs/CONFIGURATION.md) |
| Understand findings | [docs/DETECTORS.md](docs/DETECTORS.md) |
| Use reports in CI | [docs/REPORTS.md](docs/REPORTS.md) |
| Check runtime support | [docs/compatibility/compatibility-matrix.md](docs/compatibility/compatibility-matrix.md) |
| See tested server builds | [docs/compatibility/real-server-coverage.md](docs/compatibility/real-server-coverage.md) |

## Requirements

- Java 25 for the default build and release artifacts
- A Folia server for real-server integration tests
- Git and the included Gradle wrapper

## Build

```powershell
./gradlew test build
./gradlew leakTest
./gradlew performanceTest -PbenchmarkCount=25000
./gradlew release
```

The release task writes `build/release/FoliaRace-<version>.zip` and a SHA-256 checksum. The archive contains the plugin, optional agent, fixtures, harness, configuration examples, compatibility records, and `docs/`.

## Install

1. Copy the FoliaRace plugin JAR from the release archive into the server's `plugins` directory.
2. Start the server once to create the default configuration.
3. Enable the detectors and output formats you need in `plugins/FoliaRace/config.yml`.
4. Exercise the plugin workload and inspect the generated report.

The optional agent instruments selected CraftBukkit accessors without changing their return values. Keep explicit observations for plugin-specific paths outside the agent's coverage:

```powershell
java -javaagent:foliarace-agent-0.1.0.jar -jar folia-server.jar nogui
```

## Real-server harness

Run a fixture scenario against a downloaded Folia server:

```powershell
./gradlew integrationTest `
  -PfoliaServerJar=C:\servers\folia-1.21.11-14.jar `
  -PmojangServerJar=C:\servers\mojang-1.21.11.jar `
  -PmojangServerVersion=1.21.11 `
  -PfixtureScenario=cross-region-unsafe
```

Available scenarios are `cross-region-unsafe`, `same-region-safe`, and `async-state-access`. The harness creates an isolated server directory, installs the artifacts, waits for the fixture marker, and validates the report.

## CI policy

Set `ci-mode: true`, run the workload, and pass the resulting JSON report to the dedicated policy task:

```powershell
./gradlew ciCheck -PciReport=C:\path\to\report.json
```

The task fails when the report is not CI-enabled or its policy exit code is non-zero. Performance thresholds are also enforceable:

```powershell
./gradlew performanceTest `
  -PbenchmarkCount=25000 `
  -PbenchmarkMinThroughput=100000 `
  -PbenchmarkMaxDropRate=0.90
```

## Compatibility

Compile against a selected Folia API coordinate without changing source:

```powershell
./gradlew :foliarace-plugin:compileJava -PfoliaApiVersion=1.21.11-R0.1-SNAPSHOT
```

API compilation and resolver support do not replace real-server testing. Use the [compatibility matrix](docs/compatibility/compatibility-matrix.md) and [real-server coverage](docs/compatibility/real-server-coverage.md) together.

## Project documents

- [Changelog](docs/CHANGELOG.md)
- [Contributing](docs/CONTRIBUTING.md)
- [Security policy](docs/SECURITY.md)
- [Product specification](docs/FoliaRace_README.md)
