plugins {
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":foliarace-core"))
    compileOnly("dev.folia:folia-api:26.2.build.4-beta")
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
