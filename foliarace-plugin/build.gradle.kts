plugins {
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":foliarace-core"))
    val foliaApiVersion = providers.gradleProperty("foliaApiVersion").orElse("26.2.build.4-beta")
    compileOnly("dev.folia:folia-api:${foliaApiVersion.get()}")
}

tasks.shadowJar {
    archiveClassifier.set("")
    exclude("META-INF/LICENSE", "META-INF/NOTICE", "META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(mapOf("version" to project.version.toString()))
    }
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
