# Detectors

Detectors consume observations produced by explicit plugin calls or the optional instrumentation agent. They do not mutate the operation being observed.

| Detector ID | Checks | Typical evidence |
| --- | --- | --- |
| `cross-region-ownership` | A location or block operation is executed outside the owning region context. | Location, owner region, execution context |
| `cross-entity-ownership` | An entity operation is executed outside the entity's owning context. | Entity identity, owner region, execution context |
| `async-server-state-access` | Server state is accessed from an asynchronous context without an accepted ownership hand-off. | Thread/context type, operation category |
| `scheduler-misuse` | A scheduler operation does not match the selected Folia execution context. | Scheduler category, context, call site |

Unknown ownership is retained as unknown. It is not promoted to a violation solely because the runtime adapter lacks evidence. This is intentional: false certainty makes a diagnostic tool less useful.

Each finding includes a detector ID and version, severity, confidence, evidence, a call site when available, and a stable fingerprint for suppression and baseline matching. Detector output is aggregated before report writing so repeated observations do not produce unbounded report growth.

## Coverage boundaries

The agent currently covers selected world, entity, block, chunk, inventory, and server accessors in CraftBukkit implementations. Method names and implementation classes can change between server lines. Explicit observations are the reliable fallback and should be used for plugin APIs or paths that are not covered by the agent.

Real-server coverage is tracked separately from API compilation in [compatibility/real-server-coverage.md](compatibility/real-server-coverage.md).
