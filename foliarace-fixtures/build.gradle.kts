dependencies {
    compileOnly(project(":foliarace-core"))
    compileOnly(project(":foliarace-plugin"))
    compileOnly("dev.folia:folia-api:${providers.gradleProperty("foliaApiVersion").orElse("26.2.build.4-beta").get()}")
}

tasks.jar {
    archiveBaseName.set("foliarace-fixtures")
}
