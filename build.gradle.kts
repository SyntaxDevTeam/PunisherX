plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.dokka.javadoc) apply false
    alias(libs.plugins.hangar) apply false
    alias(libs.plugins.run.paper) apply false
    alias(libs.plugins.plugindeployer) apply false
}

group = "pl.syntaxdevteam.punisher"
version = "1.7.8-DEV"
description = "Advanced punishment system for Minecraft servers with commands like warn, mute, jail, ban, kick and more."

val bridgeVersion = "1.1.1-R0.1-SNAPSHOT"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka-javadoc")

    group = rootProject.group
    version = if (name.endsWith("Bridge")) bridgeVersion else rootProject.version
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds all enabled PunisherX server and proxy modules"
    val enabledBuilds = listOf(
        ":punisherx-paper",
        ":punisherx-spigot",
        ":PunisherX-Velocity-Bridge",
        ":PunisherX-BungeeCord-Bridge",
    ).filter { findProject(it) != null }
        .map { "$it:build" }
    dependsOn(enabledBuilds)
}

tasks.register("deploySpigotOnly") {
    group = "deployment"
    description = "Deploys only the Spigot module artifact"
    dependsOn(":punisherx-spigot:deployPluginToSpigot")
}
