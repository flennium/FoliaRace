import org.gradle.api.tasks.bundling.Zip
import java.security.MessageDigest

plugins {
    id("com.gradleup.shadow") version "9.0.0" apply false
}

allprojects {
    group = providers.gradleProperty("group").get()
    version = providers.gradleProperty("version").get()
}

subprojects {
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:6.0.1")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher:6.0.1")
    }
}

tasks.register("compatibilityReport") {
    group = "verification"
    description = "Prints the Folia API coordinate used for the plugin compilation."
    doLast {
        val version = providers.gradleProperty("foliaApiVersion").orElse("26.2.build.4-beta").get()
        println("FoliaRace plugin API target: dev.folia:folia-api:$version")
        println("See compatibility/compatibility-matrix.md for the explicit runtime matrix.")
    }
}

tasks.register<JavaExec>("performanceTest") {
    group = "verification"
    description = "Runs the bounded observation pipeline smoke benchmark."
    dependsOn(":foliarace-harness:jar", ":foliarace-core:jar")
    classpath = files(
        project(":foliarace-harness").tasks.named("jar"),
        project(":foliarace-core").tasks.named("jar")
    )
    mainClass.set("com.foliarace.harness.FoliaRaceBenchmark")
    args(providers.gradleProperty("benchmarkCount").orElse("100000").get())
}

tasks.register("compatibilityVerification") {
    group = "verification"
    description = "Validates that the selected Folia API target is explicitly recorded."
    dependsOn(":foliarace-plugin:compileJava")
    doLast {
        val version = providers.gradleProperty("foliaApiVersion").orElse("26.2.build.4-beta").get()
        val verified = file("compatibility/verified-api-versions.txt").readLines().map(String::trim).filter(String::isNotEmpty)
        check(version in verified) { "Folia API target $version is not in compatibility/verified-api-versions.txt" }
        println("Verified compatibility compilation target: dev.folia:folia-api:$version")
        println("Java runtime: ${System.getProperty("java.version")}")
    }
}

val releaseBundle = tasks.register<Zip>("releaseBundle") {
    group = "distribution"
    description = "Builds the reproducible FoliaRace release bundle."
    dependsOn(
        ":foliarace-plugin:shadowJar",
        ":foliarace-agent:shadowJar",
        ":foliarace-fixtures:jar",
        ":foliarace-harness:jar",
        "compatibilityVerification"
    )
    archiveFileName.set("FoliaRace-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("release"))
    from(project(":foliarace-plugin").tasks.named("shadowJar")) { into("plugins") }
    from(project(":foliarace-agent").tasks.named("shadowJar")) { into("agents") }
    from(project(":foliarace-fixtures").tasks.named("jar")) { into("fixtures") }
    from(project(":foliarace-harness").tasks.named("jar")) { into("harness") }
    from("README.md", "CHANGELOG.md", "SECURITY.md", "CONTRIBUTING.md")
    from("compatibility") { into("compatibility") }
    from("config") { into("config") }
}

tasks.register("releaseChecksums") {
    group = "distribution"
    description = "Writes a SHA-256 checksum beside the release bundle."
    dependsOn(releaseBundle)
    doLast {
        val archive = releaseBundle.get().archiveFile.get().asFile
        val digest = MessageDigest.getInstance("SHA-256").digest(archive.readBytes())
            .joinToString("") { "%02x".format(it) }
        archive.resolveSibling("${archive.name}.sha256").writeText("$digest  ${archive.name}\n")
        println("SHA-256: $digest")
    }
}

tasks.register("release") {
    group = "distribution"
    description = "Builds the release bundle and checksum."
    dependsOn("releaseChecksums")
}

tasks.register<JavaExec>("integrationTest") {
    group = "verification"
    description = "Runs a real Folia server against the fixture plugin with the instrumentation agent. Requires -PfoliaServerJar."
    dependsOn(":foliarace-plugin:shadowJar", ":foliarace-fixtures:jar", ":foliarace-agent:shadowJar", ":foliarace-harness:jar", ":foliarace-core:jar")
    val serverJar = providers.gradleProperty("foliaServerJar")
    val scenario = providers.gradleProperty("fixtureScenario").orElse("cross-region-unsafe")
    val mojangJar = providers.gradleProperty("mojangServerJar")
    onlyIf {
        if (serverJar.isPresent) true else {
            logger.lifecycle("Skipping integrationTest: pass -PfoliaServerJar=<path-to-folia-server.jar>")
            false
        }
    }
    classpath = files(
        project(":foliarace-harness").tasks.named("jar"),
        project(":foliarace-core").tasks.named("jar")
    )
    mainClass.set("com.foliarace.harness.FoliaIntegrationHarness")
    doFirst {
        if (mojangJar.isPresent) {
            jvmArgs(
                "-Dfoliarace.mojangJar=${mojangJar.get()}",
                "-Dfoliarace.mojangVersion=1.21.11"
            )
        }
    }
    doFirst {
        setArgs(listOf(
            serverJar.get(),
            project(":foliarace-plugin").layout.buildDirectory.file("libs/foliarace-plugin-${project.version}.jar").get().asFile.absolutePath,
            project(":foliarace-fixtures").layout.buildDirectory.file("libs/foliarace-fixtures-${project.version}.jar").get().asFile.absolutePath,
            scenario.get(),
            project(":foliarace-agent").layout.buildDirectory.file("libs/foliarace-agent-${project.version}.jar").get().asFile.absolutePath
        ))
    }
}
