import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.dokka.javadoc) apply false
    alias(libs.plugins.hangar) apply false
    alias(libs.plugins.run.paper) apply false
    alias(libs.plugins.plugindeployer) apply false
}

group = "pl.syntaxdevteam.punisher"
version = "1.8.1-DEV"
description = "Advanced punishment system for Minecraft servers with commands like warn, mute, jail, ban, kick and more."

val bridgeVersion = "1.1.3-R0.1-SNAPSHOT"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka-javadoc")

    group = rootProject.group
    version = if (name.endsWith("-bridge")) bridgeVersion else rootProject.version
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds all enabled PunisherX server and proxy modules"
    val enabledBuilds = listOf(
        ":punisherx-paper",
        ":punisherx-spigot",
        ":punisherx-velocity-bridge",
        ":punisherx-bungee-bridge",
    ).filter { findProject(it) != null }
        .map { "$it:build" }
    dependsOn(enabledBuilds)
}

tasks.register("deploySpigotOnly") {
    group = "deployment"
    description = "Deploys only the Spigot module artifact"
    dependsOn(":punisherx-spigot:deployPluginToSpigot")
}

// Compilation alone does not prove that the runtime database toolkit reached the plugin JAR.
subprojects {
    val verifyDatabaseBundle = tasks.register("verifyDatabaseBundle") {
        group = "verification"
        description = "Checks that the plugin JAR contains SyntaxDB, its pool and JDBC drivers"
        val bundle = tasks.named<ShadowJar>("shadowJar")
        dependsOn(bundle)
        doLast {
            val required = listOf(
                "pl/syntaxdevteam/syntaxdb/Database.class",
                "com/zaxxer/hikari/HikariDataSource.class",
                "com/mysql/cj/jdbc/Driver.class",
                "org/postgresql/Driver.class",
                "org/sqlite/JDBC.class",
                "com/microsoft/sqlserver/jdbc/SQLServerDriver.class",
                "META-INF/services/java.sql.Driver",
            )
            ZipFile(bundle.get().archiveFile.get().asFile).use { jar ->
                required.forEach { entry ->
                    check(jar.getEntry(entry) != null) { "Database runtime is incomplete: missing $entry in ${project.name}" }
                }
            }
        }
    }
    tasks.named("check") { dependsOn(verifyDatabaseBundle) }
}
