# FoliaRace project specification

## Purpose

FoliaRace provides development-time evidence for Folia ownership, execution-context, and scheduler mistakes. It is designed to help plugin authors find unsafe access during repeatable test workloads.

## Operating model

The plugin accepts explicit observations and optional agent-generated observations. A runtime adapter supplies ownership and context evidence. Detectors evaluate only the evidence they understand; unknown ownership remains unknown. Findings are aggregated, filtered, and written to versioned reports.

## Verification gates

- Unit tests cover detector and lifecycle behavior.
- `leakTest` checks that stopped diagnostic objects are collectable and worker threads terminate.
- `performanceTest` enforces throughput and queue-drop thresholds.
- `ciCheck` turns a CI-enabled report into a build exit code.
- `integrationTest` exercises the fixture plugin against a real Folia server.
- `release` builds the distributable ZIP and checksum.

## Scope boundaries

Instrumentation coverage is implementation-specific and can vary across server lines. Real-server coverage is recorded separately from API compilation. A clean report is evidence about the exercised workload, not a general safety guarantee.
