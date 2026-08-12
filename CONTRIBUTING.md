# Contributing

Use Java 25 and the Gradle wrapper. Before opening a change, run:

```powershell
./gradlew test build compatibilityVerification performanceTest -PbenchmarkCount=25000
```

For a release candidate, also run `./gradlew release`. Real-server coverage should use `integrationTest` with an explicitly supplied Folia server JAR. Keep detector behavior conservative: unknown ownership or context must not be promoted to a confirmed violation.

The local `docs/` directory contains the evolving product specification and is intentionally ignored by Git.
