# FoliaRace tutorial

This tutorial walks through a small development-server run and a CI check.

## 1. Build the artifacts

Run the unit tests and assemble the release bundle:

```powershell
./gradlew test release
```

The plugin is in `build/release/FoliaRace-0.1.0.zip`. Unpack it and copy the plugin JAR to a Folia test server. Keep the agent disabled for the first run so that explicit observations and report generation can be verified independently.

## 2. Configure a session

Start the server once, then edit the generated `plugins/FoliaRace/config.yml`:

```yaml
enabled-detectors:
  - cross-region-ownership
  - cross-entity-ownership
  - async-server-state-access
  - scheduler-misuse
output-formats:
  - json
  - markdown
ci-mode: true
```

Copy `config/suppressions.example.yml` or `config/baseline.example.json` into the same plugin data directory only when the exception is deliberate and reviewable.

## 3. Exercise the workload

The bundled fixture plugin provides three repeatable scenarios:

- `same-region-safe` performs a region-owned access from the owning region.
- `cross-region-unsafe` performs a location access from a different region and should produce an ownership finding.
- `async-state-access` reads server state from an asynchronous task and should produce an async finding.

For a real server, use the harness command in the README. The harness creates a temporary server directory, installs the artifacts, waits for the fixture marker, and validates the resulting report.

## 4. Read the report

Start with the report status and health section. Then inspect each finding's detector ID, severity, confidence, evidence, call site, and fingerprint. A finding should be fixed in the plugin under test before it is suppressed.

## 5. Enforce the result

Pass the JSON report to the dedicated CI task:

```powershell
./gradlew ciCheck -PciReport=C:\path\to\plugins\FoliaRace\reports\latest.json
```

The task exits successfully only for a report generated with `ci-mode: true` and a clean CI evaluation. This keeps server execution and build-policy enforcement separate, which is useful for both local scripts and GitHub Actions.

## 6. Add the agent when coverage is needed

After explicit observations work, add the agent to the server command. The agent instruments a bounded set of Bukkit accessors and reports through the plugin bridge. Keep the explicit path in place for operations outside that set, and compare reports before and after enabling instrumentation.
