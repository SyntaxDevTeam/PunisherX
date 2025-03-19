import io.papermc.hangarpublishplugin.model.Platforms


plugins {
    kotlin("jvm") version "2.1.20-RC2"
    id("com.gradleup.shadow") version "9.0.0-beta11"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
}

group = "pl.syntaxdevteam.punisher"
version = "1.4.0-DEV"
description = "Advanced punishment system for Minecraft servers with commands like warn, mute, jail, ban, kick and more."

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
    compileOnly("dev.folia:folia-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("org.eclipse.aether:aether-api:1.1.0")
    compileOnly("org.yaml:snakeyaml:2.4")
    compileOnly("com.google.code.gson:gson:2.12.1")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.19.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.19.0")
    compileOnly("net.kyori:adventure-text-serializer-gson:4.19.0")
    compileOnly("net.kyori:adventure-text-serializer-plain:4.19.0")
    compileOnly("net.kyori:adventure-text-serializer-ansi:4.19.0")
    compileOnly("com.maxmind.geoip2:geoip2:4.2.1")
    compileOnly("org.apache.ant:ant:1.10.15")
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.5.2")
    compileOnly("org.xerial:sqlite-jdbc:3.49.1.0")
    compileOnly("org.postgresql:postgresql:42.7.5")
    compileOnly("com.h2database:h2:2.3.232")
    compileOnly("com.zaxxer:HikariCP:6.2.1")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf(
        "version" to version,
        "description" to description
    )
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching(listOf("paper-plugin.yml")) {
        expand(props)
    }
}

hangarPublish {
    publications.register("plugin") {
        version.set(project.version as String)
        channel.set("Release")
        id.set("PunisherX")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))

        platforms {
            register(Platforms.PAPER) {
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })

                val versions: List<String> = (property("paperVersion") as String)
                    .split(",")
                    .map { it.trim() }
                platformVersions.set(versions)
            }
        }
        changelog.set(file("CHANGELOG.md").readText())
    }
}

tasks.register("prepareChangelog") {
    doLast {
        val changelogFile = File("CHANGELOG.md")
        val lines = changelogFile.readLines()
        val latestChangelog = lines
            .takeLastWhile { it.startsWith("##") }
            .joinToString("\n")
        project.extensions.getByType(io.papermc.hangarpublishplugin.HangarPublishExtension::class.java)
            .publications
            .findByName("plugin")
            ?.changelog?.set(latestChangelog)
    }
}

tasks.named("publishPluginPublicationToHangar") {
    dependsOn("prepareChangelog")
}
