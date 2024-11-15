plugins {
    kotlin("jvm") version "2.1.0-RC"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "pl.syntaxdevteam.punisher"
version = "1.1.2-DEV"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0-RC")
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly("dev.folia:folia-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("org.eclipse.aether:aether-api:1.1.0")
    compileOnly("org.yaml:snakeyaml:2.3")
    compileOnly("com.google.code.gson:gson:2.11.0")
    compileOnly("com.maxmind.geoip2:geoip2:4.2.1")
    compileOnly("org.apache.ant:ant:1.10.15")
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.5.0")
    compileOnly("org.postgresql:postgresql:42.7.4")
    compileOnly("com.h2database:h2:2.3.232")
    compileOnly("com.zaxxer:HikariCP:6.1.0")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}