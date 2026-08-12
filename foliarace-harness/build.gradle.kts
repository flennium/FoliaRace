import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.gradleup.shadow")
}

// The harness launches a real Folia server as a child process and shares core models for benchmarks.

dependencies {
    implementation(project(":foliarace-core"))
}

tasks.shadowJar {
    archiveClassifier.set("")
    exclude("META-INF/LICENSE", "META-INF/NOTICE", "META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

publishing {
    publications {
        create<MavenPublication>("harness") {
            artifact(tasks.shadowJar)
            pom {
                name.set("FoliaRace integration harness")
                description.set("Real-server integration harness and verification utilities for FoliaRace")
                url.set("https://github.com/flennium/FoliaRace")
            }
        }
    }
}
