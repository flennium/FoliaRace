# Reports and CI

FoliaRace writes a versioned JSON report and, when requested, a Markdown summary. The JSON document is the machine-readable source used by the CI task.

## Report sections

- `schemaVersion` identifies the report format.
- `sessionId`, `sessionLabel`, `generatedAt`, and `status` identify the run.
- `runtime` records the adapter, Java, compatibility profile, and available capabilities.
- `findings` contains aggregated detector results and stable fingerprints.
- `health` records queue overflow, dropped observations, lifecycle state, suppression/baseline counts, and CI evaluation.

The report records runtime compatibility status and coverage limitations so a clean report is not mistaken for complete instrumentation.

## CI enforcement

Enable `ci-mode` in the plugin configuration and run the server workload. Then invoke:

```powershell
./gradlew ciCheck -PciReport=C:\path\to\report.json
```

`ciCheck` rejects reports without `ciMode: true` and returns the policy exit code recorded by the plugin. A clean report has `ciStatus: CLEAN` and `ciExitCode: 0`; findings or an unhealthy run produce a non-zero exit code.

## Reviewing findings

Review the detector ID, evidence, context, call site, and fingerprint together. Fix the plugin first. If the behavior is intentional and cannot be changed immediately, add a narrowly scoped suppression with an owner and expiry. Use a baseline only for findings that already exist at the migration boundary.

Reports can contain server and plugin identifiers. Keep them out of public issue reports unless sensitive values have been removed.
