# Real-server coverage

This table records scenarios that have completed the isolated Folia harness. API compilation and resolver support are tracked separately in [compatibility-matrix.md](compatibility-matrix.md).

| Folia line | Build | Java | Scenarios completed | Result |
| --- | ---: | ---: | --- | --- |
| 1.21.6 | 6 | 25 | `same-region-safe`, `async-state-access` | verified |
| 1.21.8 | 6 | 25 | `same-region-safe`, `async-state-access` | verified |
| 1.21.11 | 14 | 25 | `cross-region-unsafe`, `same-region-safe`, `async-state-access` | verified |

The cross-region fixture is currently confirmed on 1.21.11 only. The older server builds completed the safe and asynchronous scenarios, but their cross-region ownership behavior did not produce the expected finding and is therefore not marked verified. That distinction is intentional.

Run one line with matching server and Mojang dependencies:

```powershell
./gradlew integrationTest `
  '-PfoliaApiVersion=1.21.11-R0.1-SNAPSHOT' `
  '-PfoliaServerJar=C:\servers\folia-1.21.11-14.jar' `
  '-PmojangServerJar=C:\servers\mojang-1.21.11.jar' `
  '-PmojangServerVersion=1.21.11' `
  '-PfixtureScenario=cross-region-unsafe'
```

The 1.19.x through 1.21.5 lines, and the 26.x lines, remain API/resolver targets until they receive dedicated server downloads and runtime scenarios. They are not real-server verified by this table.
