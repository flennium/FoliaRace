# Real-server coverage

This table records scenarios that have completed the isolated Folia harness. API compilation and resolver support are tracked separately in [compatibility-matrix.md](compatibility-matrix.md).

| Folia line | Build | Java | `same-region-safe` | `cross-region-unsafe` | `async-state-access` |
| --- | ---: | ---: | --- | --- | --- |
| 1.21.6 | 6 | 25 | passed | executed; no finding | passed |
| 1.21.8 | 6 | 25 | passed | executed; no finding | passed |
| 1.21.11 | 14 | 25 | passed | passed; finding detected | passed |

All three fixture scenarios were executed against all three available server builds. The 1.21.6 and 1.21.8 servers completed the cross-region workload but did not expose the ownership evidence required by the detector; those cells are intentionally not marked as passing. The harness reports this as an explicit detector gap and preserves the failed run artifacts instead of hiding the result.

Run one line with matching server and Mojang dependencies:

```powershell
./gradlew integrationTestAll `
  '-PfoliaApiVersion=1.21.11-R0.1-SNAPSHOT' `
  '-PfoliaServerJar=C:\servers\folia-1.21.11-14.jar' `
  '-PmojangServerJar=C:\servers\mojang-1.21.11.jar' `
  '-PmojangServerVersion=1.21.11'
```

Use `integrationTest -PfixtureScenario=<id>` for one scenario. Valid IDs are `same-region-safe`, `cross-region-unsafe`, and `async-state-access`; arbitrary values are rejected. Failed runs retain their temporary server directory, including `server.log`, plugin logs, and reports.

The manual GitHub Actions workflow `Real-server coverage` runs the same three IDs in parallel when supplied with licensed Folia and matching Mojang JAR URLs. Failed workflow jobs upload the preserved temporary server artifacts.

The 1.21.4, 1.21.5, and 26.x lines remain API/resolver targets until they receive dedicated server downloads and runtime scenarios. Older than 1.21.4 is outside the current release artifact contract because the plugin is compiled for Java 25 and declares API version 1.21.
