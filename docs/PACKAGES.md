# Package distribution

FoliaRace uses two distribution channels because they solve different problems:

- GitHub Packages is the dependency-friendly channel for individual Maven artifacts.
- The release ZIP is the practical server bundle when the plugin, agent, fixtures, harness, configuration examples, and documentation are needed together.

## GitHub Packages

Packages are published by `.github/workflows/publish.yml` when a GitHub release is published. The workflow uses the repository-scoped `GITHUB_TOKEN` with `packages: write`; no long-lived credential is stored in the repository.

The published coordinates use the project group `com.foliarace`:

```text
com.foliarace:foliarace-core:0.1.0
com.foliarace:foliarace-plugin:0.1.0
com.foliarace:foliarace-agent:0.1.0
com.foliarace:foliarace-fixtures:0.1.0
com.foliarace:foliarace-harness:0.1.0
```

Configure a consuming Gradle build with credentials supplied outside source control:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/flennium/FoliaRace")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull
                ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull
                ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    compileOnly("com.foliarace:foliarace-plugin:0.1.0")
}
```

Declare the runtime plugin dependency in the consuming plugin's `plugin.yml`:

```yaml
depend:
  - FoliaRace
```

Do not use `implementation` for the FoliaRace plugin artifact. Bundling it into another plugin creates duplicate classes and separate plugin state; Folia should load one FoliaRace plugin instance.

GitHub Packages Maven/Gradle access requires authentication. Use a personal access token classic with the appropriate package scope for local access, or `GITHUB_TOKEN` inside a workflow with package permissions.

## Local publishing

To publish all module publications from a developer machine:

```powershell
./gradlew publishAllPublicationsToGitHubPackagesRepository `
  -Pgpr.user=$env:GITHUB_ACTOR `
  -Pgpr.key=$env:GITHUB_TOKEN
```

The root build verifies module artifacts and plugin descriptors. The manual `Example consumer smoke test` workflow initializes the linked standalone example, publishes the plugin to Maven Local, and builds the example with `compileOnly` to verify the public consumer boundary without bundling FoliaRace.

For normal server installation, download the standalone FoliaRace plugin JAR from the GitHub Release assets, or use the release ZIP for the complete operational bundle. The ZIP is not a replacement for the Maven repository; it is the assembled operational bundle.
