# Configuration reference

The plugin copies the defaults from `foliarace-plugin/src/main/resources/config.yml` into its data directory on first start.

| Key | Meaning |
| --- | --- |
| `enabled-detectors` | Detector IDs to load. Unknown IDs reject the configuration. |
| `overhead-mode` | Observation overhead profile, currently `standard` or the supported lower-overhead profiles. |
| `max-session-duration-seconds` | Maximum lifetime of one diagnostic session. |
| `minimum-severity` | Lowest finding severity written to the report. |
| `minimum-confidence` | Lowest finding confidence written to the report. |
| `observation-queue-capacity` | Bounded pipeline queue size. Overflow is counted in report health. |
| `sampling-rate` | Fraction of eligible observations accepted, from `0.0` through `1.0`. |
| `output-formats` | Report formats such as `json` and `markdown`. |
| `production-mode` | Enables the guarded production mode. Use only with an explicit acknowledgement. |
| `production-acknowledged` | Must be true before production mode is accepted. |
| `suppression-file` | File name for reviewed, expiring suppressions. |
| `baseline-file` | File name for the comparison baseline. |
| `ci-mode` | Adds a CI evaluation to the report and makes unsuppressed findings policy failures. |

Configuration is validated before the plugin starts its detectors. Invalid values disable FoliaRace rather than silently selecting unsafe defaults.

## Suppressions

Use `config/suppressions.example.yml` as the starting point. Every suppression should identify the detector and plugin, include a reason, and have an expiry date. Suppression is an exception workflow, not a replacement for fixing the underlying access.

## Baselines

Use `config/baseline.example.json` to record an approved existing state while a plugin is being migrated. New fingerprints remain visible, and stale baseline entries are reported so the file does not become permanent blind coverage.
