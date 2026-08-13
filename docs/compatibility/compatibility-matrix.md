# FoliaRace compatibility matrix

This matrix is executable in the core resolver and is intentionally explicit. The published FoliaRace artifact is compiled for Java 25 and declares Bukkit API version 1.21, so this release supports the 1.21.4+ and 26.x lines listed below. Older API coordinates are not supported by this artifact.

| Folia line | API coordinate | Minimum Java | Resolver profile | Initial status |
| --- | --- | ---: | --- | --- |
| 1.21.4 | `1.21.4-R0.1-SNAPSHOT` | 25 | `folia-1.21.4` | supported when scheduler capabilities are present |
| 1.21.5 | `1.21.5-R0.1-SNAPSHOT` | 25 | `folia-1.21.5` | supported when scheduler capabilities are present |
| 1.21.6 | `1.21.6-R0.1-SNAPSHOT` | 25 | `folia-1.21.6` | supported when scheduler capabilities are present |
| 1.21.8 | `1.21.8-R0.1-SNAPSHOT` | 25 | `folia-1.21.8` | supported when scheduler capabilities are present |
| 1.21.11 | `1.21.11-R0.1-SNAPSHOT` | 25 | `folia-1.21.11` | supported when scheduler capabilities are present |
| 26.1 | `26.1.2.build.8-stable` | 25 | `folia-26.1.2` | supported when scheduler capabilities are present |
| 26.2 | `26.2.build.4-beta` | 25 | `folia-26.2` | supported when scheduler capabilities are present |

## Build against a specific API line

The plugin module accepts a version override without changing source:

```powershell
./gradlew :foliarace-plugin:compileJava -PfoliaApiVersion=1.21.11-R0.1-SNAPSHOT
./gradlew :foliarace-plugin:compileJava -PfoliaApiVersion=26.1.2.build.8-stable
```

The default build remains pinned to `26.2.build.4-beta` for reproducibility. Resolver support and API compilation do not imply real-server coverage. See [`real-server-coverage.md`](real-server-coverage.md) for the versions that have completed the isolated harness. Supporting older Java/API lines requires a separately compiled artifact; the current release does not claim those lines.
