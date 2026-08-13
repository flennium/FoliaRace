import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.api.publish.PublishingExtension
import java.security.MessageDigest

plugins {
    id("com.gradleup.shadow") version "9.0.0" apply false
}

allprojects {
    group = providers.gradleProperty("group").get()
    version = providers.gradleProperty("version").get()
}

val foliaraceJavaVersion = providers.gradleProperty("foliaraceJavaVersion").map(String::toInt).orElse(25)

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/flennium/FoliaRace")
                credentials {
                    username = providers.gradleProperty("gpr.user")
                        .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                        .orNull
                    password = providers.gradleProperty("gpr.key")
                        .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                        .orNull
                }
            }
        }
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(foliaraceJavaVersion.get()))
        }
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(foliaraceJavaVersion.get())
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

val javaToolchainService = project(":foliarace-core").extensions.getByType<JavaToolchainService>()

tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(javaToolchainService.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(foliaraceJavaVersion.get()))
    })
}

tasks.register("compatibilityReport") {
    group = "verification"
    description = "Prints the Folia API coordinate used for the plugin compilation."
    doLast {
        val version = providers.gradleProperty("foliaApiVersion").orElse("26.2.build.4-beta").get()
        println("FoliaRace plugin API target: dev.folia:folia-api:$version")
        println("See docs/compatibility/compatibility-matrix.md for the explicit runtime matrix.")
    }
}

tasks.register<JavaExec>("performanceTest") {
    group = "verification"
    description = "Runs the bounded observation pipeline smoke benchmark."
    dependsOn(":foliarace-harness:shadowJar")
    classpath = files(
        project(":foliarace-harness").tasks.named("shadowJar")
    )
    mainClass.set("com.foliarace.harness.FoliaRaceBenchmark")
    args(providers.gradleProperty("benchmarkCount").orElse("100000").get())
    systemProperty("foliarace.benchmark.minThroughput", providers.gradleProperty("benchmarkMinThroughput").orElse("100000").get())
    systemProperty("foliarace.benchmark.maxDropRate", providers.gradleProperty("benchmarkMaxDropRate").orElse("0.90").get())
}

tasks.register<JavaExec>("ciCheck") {
    group = "verification"
    description = "Fails the build when a FoliaRace report violates CI policy."
    val report = providers.gradleProperty("ciReport")
    dependsOn(":foliarace-harness:shadowJar")
    doFirst {
        check(report.isPresent) { "Pass -PciReport=<path-to-report.json>" }
        check(file(report.get()).isFile) { "CI report does not exist: ${report.get()}" }
    }
    classpath = files(
        project(":foliarace-harness").tasks.named("shadowJar")
    )
    mainClass.set("com.foliarace.harness.FoliaRaceCi")
    doFirst {
        setArgs(listOf(report.get()))
    }
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
        ":foliarace-harness:shadowJar",
        "compatibilityVerification"
    )
    archiveFileName.set("FoliaRace-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("release"))
    from(project(":foliarace-plugin").tasks.named("shadowJar")) { into("plugins") }
    from(project(":foliarace-agent").tasks.named("shadowJar")) { into("agents") }
    from(project(":foliarace-fixtures").tasks.named("jar")) { into("fixtures") }
    from(project(":foliarace-harness").tasks.named("shadowJar")) { into("harness") }
    from("README.md")
    from("docs") { into("docs") }
    from("ci") { into("ci") }
    from("compatibility/verified-api-versions.txt") { into("compatibility") }
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
    dependsOn("releaseChecksums", "leakTest")
}

tasks.register("leakTest") {
    group = "verification"
    description = "Runs lifecycle leak detection as an explicit release gate."
    dependsOn(":foliarace-core:test")
    doLast {
        logger.lifecycle("Lifecycle leak gate passed")
    }
}

tasks.register<JavaExec>("integrationTest") {
    group = "verification"
    description = "Runs a real Folia server against the fixture plugin with the instrumentation agent. Requires -PfoliaServerJar."
    dependsOn(":foliarace-plugin:shadowJar", ":foliarace-fixtures:jar", ":foliarace-agent:shadowJar", ":foliarace-harness:shadowJar")
    val serverJar = providers.gradleProperty("foliaServerJar")
    val scenario = providers.gradleProperty("fixtureScenario").orElse("cross-region-unsafe")
    val mojangJar = providers.gradleProperty("mojangServerJar")
    val mojangVersion = providers.gradleProperty("mojangServerVersion").orElse("1.21.11")
    onlyIf {
        if (serverJar.isPresent) true else {
            logger.lifecycle("Skipping integrationTest: pass -PfoliaServerJar=<path-to-folia-server.jar>")
            false
        }
    }
    classpath = files(
        project(":foliarace-harness").tasks.named("shadowJar")
    )
    mainClass.set("com.foliarace.harness.FoliaIntegrationHarness")
    doFirst {
        if (mojangJar.isPresent) {
            jvmArgs(
                "-Dfoliarace.mojangJar=${mojangJar.get()}",
                "-Dfoliarace.mojangVersion=${mojangVersion.get()}"
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

tasks.register<JavaExec>("integrationTestAll") {
    group = "verification"
    description = "Runs every declared fixture scenario against one supplied Folia server. Requires -PfoliaServerJar."
    dependsOn(":foliarace-plugin:shadowJar", ":foliarace-fixtures:jar", ":foliarace-agent:shadowJar", ":foliarace-harness:shadowJar")
    val serverJar = providers.gradleProperty("foliaServerJar")
    val mojangJar = providers.gradleProperty("mojangServerJar")
    val mojangVersion = providers.gradleProperty("mojangServerVersion").orElse("1.21.11")
    onlyIf {
        if (serverJar.isPresent) true else {
            logger.lifecycle("Skipping integrationTestAll: pass -PfoliaServerJar=<path-to-folia-server.jar>")
            false
        }
    }
    classpath = files(project(":foliarace-harness").tasks.named("shadowJar"))
    mainClass.set("com.foliarace.harness.FoliaIntegrationHarness")
    doFirst {
        if (mojangJar.isPresent) {
            jvmArgs("-Dfoliarace.mojangJar=${mojangJar.get()}", "-Dfoliarace.mojangVersion=${mojangVersion.get()}")
        }
        setArgs(listOf(
                serverJar.get(),
                project(":foliarace-plugin").layout.buildDirectory.file("libs/foliarace-plugin-${project.version}.jar").get().asFile.absolutePath,
                project(":foliarace-fixtures").layout.buildDirectory.file("libs/foliarace-fixtures-${project.version}.jar").get().asFile.absolutePath,
                "all",
                project(":foliarace-agent").layout.buildDirectory.file("libs/foliarace-agent-${project.version}.jar").get().asFile.absolutePath
        ))
    }
}
