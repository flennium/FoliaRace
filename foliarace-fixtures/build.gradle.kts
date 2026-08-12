import org.gradle.api.publish.maven.MavenPublication

dependencies {
    compileOnly(project(":foliarace-core"))
    compileOnly(project(":foliarace-plugin"))
    compileOnly("dev.folia:folia-api:${providers.gradleProperty("foliaApiVersion").orElse("26.2.build.4-beta").get()}")
}

tasks.jar {
    archiveBaseName.set("foliarace-fixtures")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(mapOf("version" to project.version.toString()))
    }
}

publishing {
    publications {
        create<MavenPublication>("fixtures") {
            artifact(tasks.jar)
            pom {
                name.set("FoliaRace fixture plugin")
                description.set("Test-only Folia fixture scenarios for FoliaRace")
                url.set("https://github.com/flennium/FoliaRace")
            }
        }
    }
}
