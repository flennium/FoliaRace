# Reports and CI

FoliaRace writes a versioned JSON report and, when requested, a Markdown summary. The JSON document is the machine-readable source used by the CI task.

## Report sections

- `schemaVersion` identifies the report format.
- `sessionId`, `sessionLabel`, `generatedAt`, and `status` identify the run.
- `runtime` records the adapter, Java, compatibility profile, and available capabilities.
- `findings` contains aggregated detector results and stable fingerprints.
- `health` records queue overflow, dropped observations, lifecycle timestamps, suppression/baseline counts, instrumentation coverage, runtime coverage, and CI evaluation.

The report records runtime compatibility status and coverage limitations so a clean report is not mistaken for complete instrumentation.

Schema `1` is the current contract. Additive health fields are compatible within schema `1`; changing field meaning or removing fields requires a new schema version. Markdown is a human-readable projection of the same finding and health facts, not a separate data contract.

## CI enforcement

Enable `ci-mode` in the plugin configuration and run the server workload. Then invoke:

```powershell
./gradlew ciCheck -PciReport=C:\path\to\report.json
```

`ciCheck` rejects reports without `ciMode: true` and returns the policy exit code recorded by the plugin. A clean report has `ciStatus: CLEAN` and `ciExitCode: 0`; findings or an unhealthy run produce a non-zero exit code.

## Reviewing findings

Review the detector ID, evidence, context, call site, and fingerprint together. Fix the plugin first. If the behavior is intentional and cannot be changed immediately, add a narrowly scoped suppression with an owner and expiry. Use a baseline only for findings that already exist at the migration boundary.

Reports can contain server and plugin identifiers. Keep them out of public issue reports unless sensitive values have been removed.

## Session lifecycle

`/foliarace start` starts a new session only when none is active; an active session is left unchanged. `/foliarace stop` stops the active session and reports when no session is active. `/foliarace flush` writes a snapshot without changing session state. The configured maximum duration automatically stops and flushes the session. Reports are named by session ID, while `reports/latest.json` and `reports/latest.md` point to the most recent selected formats; older session files are rotated according to `report-retention-count`.
