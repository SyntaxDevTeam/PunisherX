rootProject.name = "PunisherX"

pluginManagement {
    repositories {
        maven("https://nexus.syntaxdevteam.pl/repository/maven-releases/")
        maven("https://nexus.syntaxdevteam.pl/repository/maven-snapshots/")

        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(
    "punisherx-api",
    "punisherx-core",
    "punisherx-compat",
    "punisherx-platform-paper",
    "punisherx-platform-folia",
    "punisherx-platform-velocity",
    "punisherx-addons:punisherx-addons-discord",
    "punisherx-addons:punisherx-addons-gui-extra",
    "punisherx-addons:punisherx-addons-webpanel",
    "bungee-bridge",
    "velocity-bridge"
)
