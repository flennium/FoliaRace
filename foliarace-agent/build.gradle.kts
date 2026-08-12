import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.gradleup.shadow")
}

dependencies {
    implementation("net.bytebuddy:byte-buddy:1.18.11")
}

tasks.jar {
    manifest {
        attributes[
            "Premain-Class"] = "com.foliarace.agent.FoliaRaceAgent"
        attributes["Agent-Class"] = "com.foliarace.agent.FoliaRaceAgent"
        attributes["Can-Redefine-Classes"] = "true"
        attributes["Can-Retransform-Classes"] = "true"
    }
}

tasks.shadowJar {
    archiveBaseName.set("foliarace-agent")
    archiveClassifier.set("")
    manifest.inheritFrom(tasks.jar.get().manifest)
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

publishing {
    publications {
        create<MavenPublication>("agent") {
            artifact(tasks.shadowJar)
            pom {
                name.set("FoliaRace instrumentation agent")
                description.set("Optional Byte Buddy instrumentation agent for FoliaRace")
                url.set("https://github.com/flennium/FoliaRace")
            }
        }
    }
}
