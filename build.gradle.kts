plugins {
    kotlin("jvm") version "2.4.0" apply false
    id("com.gradleup.shadow") version "9.4.2" apply false
    id("org.jetbrains.dokka-javadoc") version "2.2.0" apply false
    id("io.papermc.hangar-publish-plugin") version "0.1.4" apply false
    id("xyz.jpenilla.run-paper") version "3.0.2" apply false
    id("pl.syntaxdevteam.plugindeployer") version "1.0.5-R0.1-SNAPSHOT" apply false
}

group = "pl.syntaxdevteam.punisher"
version = property("punisherxVersion") as String
description = "Advanced punishment system for Minecraft servers with commands like warn, mute, jail, ban, kick and more."

val bridgeVersion = property("bridgeVersion") as String

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka-javadoc")

    group = rootProject.group
    version = if (name.endsWith("Bridge")) bridgeVersion else rootProject.version
}
