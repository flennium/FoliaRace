# Security policy

FoliaRace is a development-time diagnostic tool. Review the enabled instrumentation targets before attaching the agent to any server that handles untrusted plugins or player-controlled data.

Reports may include plugin names, class names, locations, and server version details. Treat generated reports as operational data and remove secrets before sharing them.

To report a vulnerability, open a private GitHub security advisory for `flennium/FoliaRace` when possible. Include the affected version, runtime/API version, configuration, and a minimal reproduction. Do not publish credentials, server logs containing secrets, or private plugin source.
