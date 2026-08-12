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

tasks.register<JavaExec>("integrationTest") {
    group = "verification"
    description = "Runs a real Folia server against the fixture plugin with the instrumentation agent. Requires -PfoliaServerJar."
    dependsOn(":foliarace-plugin:shadowJar", ":foliarace-fixtures:jar", ":foliarace-agent:shadowJar", ":foliarace-harness:classes")
    val serverJar = providers.gradleProperty("foliaServerJar")
    val scenario = providers.gradleProperty("fixtureScenario").orElse("cross-region-unsafe")
    onlyIf {
        if (serverJar.isPresent) true else {
            logger.lifecycle("Skipping integrationTest: pass -PfoliaServerJar=<path-to-folia-server.jar>")
            false
        }
    }
    classpath = project(":foliarace-harness").extensions.getByType<SourceSetContainer>()
        .getByName("main").runtimeClasspath
    mainClass.set("com.foliarace.harness.FoliaIntegrationHarness")
    args(
        serverJar.map { it },
        project(":foliarace-plugin").layout.buildDirectory.file("libs/FoliaRace-${project.version}.jar").get().asFile.absolutePath,
        project(":foliarace-fixtures").layout.buildDirectory.file("libs/foliarace-fixtures-${project.version}.jar").get().asFile.absolutePath,
        scenario.get(),
        project(":foliarace-agent").layout.buildDirectory.file("libs/foliarace-agent-${project.version}.jar").get().asFile.absolutePath
    )
}
