plugins {
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "9.3.1"
}

group = "pl.syntaxdevteam.punisher"
version = rootProject.version

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/releases/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.md-5.net/content/groups/public/")
    maven("https://libraries.minecraft.net")
}

dependencies {
    compileOnly("net.md-5:bungeecord-api:1.21-R0.4")
}

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("net.md-5:brigadier")).using(module("com.mojang:brigadier:1.0.500"))
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    filesMatching("bungee.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.shadowJar {
    archiveBaseName.set("PunisherX-Bridge")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
