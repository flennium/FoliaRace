# Real-server coverage

This table records scenarios that have completed the isolated Folia harness. API compilation and resolver support are tracked separately in [compatibility-matrix.md](compatibility-matrix.md).

| Folia line | Build | Java | `same-region-safe` | `cross-region-unsafe` | `async-state-access` |
| --- | ---: | ---: | --- | --- | --- |
| 1.21.6 | 6 | 25 | passed | executed; no finding | passed |
| 1.21.8 | 6 | 25 | passed | executed; no finding | passed |
| 1.21.11 | 14 | 25 | passed | passed; finding detected | passed |

All three fixture scenarios were executed against all three available server builds. The 1.21.6 and 1.21.8 servers completed the cross-region workload but did not expose the ownership evidence required by the detector; those cells are intentionally not marked as passing. The harness therefore exits non-zero for those two expected detector gaps rather than hiding them.

Run one line with matching server and Mojang dependencies:

```powershell
./gradlew integrationTest `
  '-PfoliaApiVersion=1.21.11-R0.1-SNAPSHOT' `
  '-PfoliaServerJar=C:\servers\folia-1.21.11-14.jar' `
  '-PmojangServerJar=C:\servers\mojang-1.21.11.jar' `
  '-PmojangServerVersion=1.21.11' `
  '-PfixtureScenario=cross-region-unsafe'
```

The 1.21.4, 1.21.5, and 26.x lines remain API/resolver targets until they receive dedicated server downloads and runtime scenarios. Older than 1.21.4 is outside the current release artifact contract because the plugin is compiled for Java 25 and declares API version 1.21.
