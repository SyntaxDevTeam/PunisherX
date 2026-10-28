import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.zip.ZipFile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.dokka.javadoc) apply false
    alias(libs.plugins.hangar) apply false
    alias(libs.plugins.run.paper) apply false
    alias(libs.plugins.plugindeployer) apply false
}

group = "pl.syntaxdevteam.punisher"
version = "1.9.0-DEV"
description = "Advanced punishment system for Minecraft servers with commands like warn, mute, jail, ban, kick and more."

val bridgeVersion = "1.1.3-R0.1-SNAPSHOT"
val databaseRuntimeDependencies = listOf(libs.syntaxdb.core, libs.hikari, libs.mysql.jdbc, libs.protobuf,
    libs.postgresql.jdbc, libs.mssql.jdbc, libs.sqlite.jdbc, libs.kotlin.stdlib)

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka-javadoc")

    group = rootProject.group
    version = if (name.endsWith("-bridge")) bridgeVersion else rootProject.version

    if (name in setOf("punisherx-paper", "punisherx-spigot")) {
        extensions.configure<KotlinJvmProjectExtension> {
            sourceSets.named("test") {
                kotlin.srcDir(rootProject.file("tests/server/kotlin"))
            }
        }
        // Mirrors the explicit runtime manifests, without optional mapping/coroutine modules.
        val databaseRuntimeLibraries = configurations.create("databaseRuntimeLibraries")
        databaseRuntimeDependencies.forEach {
            dependencies.add(databaseRuntimeLibraries.name, it)
        }
        tasks.withType<Test>().configureEach {
            inputs.files(databaseRuntimeLibraries)
            inputs.property("mysqlTestUrl", providers.environmentVariable("PUNISHERX_TEST_MYSQL_URL").orElse(""))
            doFirst {
                systemProperty("punisherx.database.runtimeClasspath", databaseRuntimeLibraries.asPath)
            }
        }
    }
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

// Server plugins download their DB runtime; proxies currently bundle it (no runtime loader).
subprojects {
    val verifyDatabaseBundle = tasks.register("verifyDatabaseBundle") {
        group = "verification"
        description = "Checks the database runtime packaging contract for server and proxy JARs"
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
                if (project.name in setOf("punisherx-paper", "punisherx-spigot")) {
                    val forbidden = listOf("pl/syntaxdevteam/syntaxdb/", "com/zaxxer/hikari/", "org/sqlite/",
                        "com/mysql/", "org/postgresql/", "com/microsoft/sqlserver/", "kotlin/", "kotlinx/coroutines/")
                    check(jar.entries().asSequence().none { entry -> forbidden.any { entry.name.startsWith(it) } }) {
                        "Downloaded libraries must not be bundled in ${project.name}"
                    }
                    val platform = project.name.removePrefix("punisherx-")
                    val manifest = requireNotNull(jar.getEntry("$platform-libraries.yml"))
                    val text = jar.getInputStream(manifest).bufferedReader().use { it.readText() }
                    check(!text.contains("\${")) { "Unexpanded runtime library versions in ${project.name}" }
                    listOf("pl.syntaxdevteam:syntaxdb-core:", "com.zaxxer:HikariCP:",
                        "com.mysql:mysql-connector-j:", "com.google.protobuf:protobuf-java:",
                        "org.postgresql:postgresql:", "com.microsoft.sqlserver:mssql-jdbc:", "org.xerial:sqlite-jdbc:")
                        .forEach { check(text.contains(it)) { "Missing runtime library $it in ${project.name}" } }
                    if (platform == "spigot") {
                        val descriptor = jar.getInputStream(jar.getEntry("plugin.yml")).bufferedReader().use { it.readText() }
                        check(descriptor.contains("org.jetbrains.kotlin:kotlin-stdlib:")) { "Spigot must bootstrap Kotlin before Libby" }
                    }
                } else {
                    required.forEach { entry ->
                        check(jar.getEntry(entry) != null) { "Database runtime is incomplete: missing $entry in ${project.name}" }
                    }
                }
            }
        }
    }
    tasks.named("check") { dependsOn(verifyDatabaseBundle) }
}
