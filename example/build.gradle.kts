dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation(project(":"))
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching(listOf("paper-plugin.yml")) {
        expand(mapOf("version" to project.version.toString()))
    }
}
