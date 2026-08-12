# FoliaRace compatibility matrix

This matrix is executable in the core resolver and is intentionally explicit. A runtime is not treated as supported merely because its version resembles a known line.

| Folia line | API coordinate | Minimum Java | Resolver profile | Initial status |
| --- | --- | ---: | --- | --- |
| 1.19.4 | `1.19.4-R0.1-SNAPSHOT` | 17 | `folia-1.19.4` | supported when scheduler capabilities are present |
| 1.20.1 | `1.20.1-R0.1-SNAPSHOT` | 17 | `folia-1.20.1` | supported when scheduler capabilities are present |
| 1.20.2 | `1.20.2-R0.1-SNAPSHOT` | 17 | `folia-1.20.2` | supported when scheduler capabilities are present |
| 1.20.4 | `1.20.4-R0.1-SNAPSHOT` | 17 | `folia-1.20.4` | supported when scheduler capabilities are present |
| 1.20.6 | `1.20.6-R0.1-SNAPSHOT` | 21 | `folia-1.20.6` | supported when scheduler capabilities are present |
| 1.21.4 | `1.21.4-R0.1-SNAPSHOT` | 21 | `folia-1.21.4` | supported when scheduler capabilities are present |
| 1.21.5 | `1.21.5-R0.1-SNAPSHOT` | 21 | `folia-1.21.5` | supported when scheduler capabilities are present |
| 1.21.6 | `1.21.6-R0.1-SNAPSHOT` | 21 | `folia-1.21.6` | supported when scheduler capabilities are present |
| 1.21.8 | `1.21.8-R0.1-SNAPSHOT` | 21 | `folia-1.21.8` | supported when scheduler capabilities are present |
| 1.21.11 | `1.21.11-R0.1-SNAPSHOT` | 21 | `folia-1.21.11` | supported when scheduler capabilities are present |
| 26.1 | `26.1.2.build.8-stable` | 25 | `folia-26.1.2` | supported when scheduler capabilities are present |
| 26.2 | `26.2.build.4-beta` | 25 | `folia-26.2` | supported when scheduler capabilities are present |

## Build against a specific API line

The plugin module accepts a version override without changing source:

```powershell
./gradlew :foliarace-plugin:compileJava -PfoliaApiVersion=1.21.11-R0.1-SNAPSHOT
./gradlew :foliarace-plugin:compileJava -PfoliaApiVersion=26.1.2.build.8-stable
```

The default build remains pinned to `26.2.build.4-beta` for reproducibility. Resolver support and API compilation do not imply real-server coverage. See [`real-server-coverage.md`](real-server-coverage.md) for the versions that have completed the isolated harness.
