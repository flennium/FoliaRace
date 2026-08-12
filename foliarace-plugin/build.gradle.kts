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
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
