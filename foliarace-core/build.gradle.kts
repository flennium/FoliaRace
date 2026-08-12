import org.gradle.api.publish.maven.MavenPublication

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.20.2")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.2")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
