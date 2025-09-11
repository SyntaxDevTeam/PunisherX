rootProject.name = "PunisherX"
pluginManagement {
    repositories {
        // Twoje repozytoria z pluginami:
        maven("https://nexus.syntaxdevteam.pl/repository/maven-releases/")
        maven("https://nexus.syntaxdevteam.pl/repository/maven-snapshots/")

        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}