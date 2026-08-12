# Contributing

Use Java 25 for compilation and the Gradle wrapper for builds. Keep changes focused and preserve conservative behavior when ownership or context evidence is incomplete.

## Verification

```powershell
./gradlew test build compatibilityVerification leakTest
./gradlew performanceTest -PbenchmarkCount=25000 -PbenchmarkMinThroughput=100000 -PbenchmarkMaxDropRate=0.90
```

When changing runtime behavior, run the real-server harness for the affected Folia line and update `compatibility/real-server-coverage.md` with the exact server build and scenarios executed. Do not mark a version as server-verified from API compilation alone.

For a release candidate:

```powershell
./gradlew release
```

The local `docs/` directory contains the evolving product specification and is intentionally ignored by Git. The public project documentation lives in the tracked Markdown files at the repository root and under `compatibility/`.
