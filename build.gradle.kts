import io.papermc.hangarpublishplugin.model.Platforms
import org.gradle.api.publish.maven.MavenPublication

plugins {
    kotlin("jvm") version "2.2.0-RC2"
    id("com.gradleup.shadow") version "9.0.0-beta15"
    `maven-publish`
    id("io.papermc.hangar-publish-plugin") version "0.1.3"
}

group = "pl.syntaxdevteam.punisher"
version = "1.4.1-DEV"
description = "Advanced punishment system for Minecraft servers with commands like warn, mute, jail, ban, kick and more."

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://repo.extendedclip.com/releases/") // PlaceholderAPI
    maven("https://repo.codemc.org/repository/maven-public") // VaultUnlockedAPI
    maven("https://jitpack.io") // VaultAPI
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly("org.eclipse.aether:aether-api:1.1.0")
    compileOnly("org.yaml:snakeyaml:2.4")
    compileOnly("com.google.code.gson:gson:2.13.1")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.21.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.21.0")
    compileOnly("net.kyori:adventure-text-serializer-gson:4.21.0")
    compileOnly("net.kyori:adventure-text-serializer-plain:4.21.0")
    compileOnly("net.kyori:adventure-text-serializer-ansi:4.21.0")
    compileOnly("com.maxmind.geoip2:geoip2:4.3.1")
    compileOnly("org.apache.ant:ant:1.10.15")
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.5.3")
    compileOnly("org.xerial:sqlite-jdbc:3.49.1.0")
    compileOnly("org.postgresql:postgresql:42.7.6")
    compileOnly("com.h2database:h2:2.3.232")
    compileOnly("com.zaxxer:HikariCP:6.3.0")
    compileOnly("net.luckperms:api:5.5")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("io.github.miniplaceholders:miniplaceholders-kotlin-ext:2.3.0")
    compileOnly("com.github.milkbowl:VaultAPI:1.7.1")
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.11")
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.2.0")
}

tasks {
    build {
        dependsOn(shadowJar)
    }
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

// ShadowJar configuration
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("PunisherX")
    archiveClassifier.set("")      // usuwa domyślne "-all"
    archiveVersion.set(project.version.toString())
    mergeServiceFiles()
}

publishing {
    publications {
        create<MavenPublication>("PunisherX") {
            artifact(tasks.named("shadowJar").get()) {
                classifier = null
            }
            pom {
                name.set("PunisherX")
                description.set(project.description)
                url.set("https://github.com/TwojRepo/PunisherX")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("WieszczY85")
                        name.set("WieszczY")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "Nexus"
            url = uri("https://nexus.syntaxdevteam.pl/repository/maven-releases/")
            credentials {
                username = findProperty("nexusUser")?.toString()
                    ?: throw GradleException("Właściwość 'nexusUser' nie jest ustawiona w gradle.properties")
                password = findProperty("nexusPassword")?.toString()
                    ?: throw GradleException("Właściwość 'nexusPassword' nie jest ustawiona w gradle.properties")
            }
        }
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
