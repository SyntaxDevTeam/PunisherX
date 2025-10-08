import io.papermc.hangarpublishplugin.model.Platforms
import org.gradle.api.publish.maven.MavenPublication


plugins {
    kotlin("jvm") version "2.2.20"
    id("com.gradleup.shadow") version "9.2.2"
    `maven-publish`
    id("io.papermc.hangar-publish-plugin") version "0.1.3"
    id("xyz.jpenilla.run-paper") version "3.0.0"
    id("pl.syntaxdevteam.plugindeployer") version "1.0.1"
}

group = "pl.syntaxdevteam.punisher"
version = "1.5.1"
description = "Advanced punishment system for Minecraft servers with commands like warn, mute, jail, ban, kick and more."

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://repo.extendedclip.com/releases/") // PlaceholderAPI
    maven("https://repo.codemc.org/repository/maven-public/") // VaultUnlockedAPI
    maven("https://jitpack.io") // VaultAPI
    maven("https://nexus.syntaxdevteam.pl/repository/maven-snapshots/") //SyntaxDevTeam
    maven("https://nexus.syntaxdevteam.pl/repository/maven-releases/") //SyntaxDevTeam
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    //compileOnly("dev.folia:folia-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("pl.syntaxdevteam:core:1.2.4-SNAPSHOT")
    //compileOnly("pl.syntaxdevteam.license:license-client:0.1.1")
    //implementation(files("libs/SyntaxCore-1.1.0-all.jar"))
    compileOnly("pl.syntaxdevteam:cleanerx:1.5.2")
    compileOnly("org.eclipse.aether:aether-api:1.1.0")
    compileOnly("org.yaml:snakeyaml:2.5")
    compileOnly("com.google.code.gson:gson:2.13.2")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.24.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.24.0")
    compileOnly("net.kyori:adventure-text-serializer-gson:4.24.0")
    compileOnly("net.kyori:adventure-text-serializer-plain:4.24.0")
    compileOnly("net.kyori:adventure-text-serializer-ansi:4.24.0")
    compileOnly("net.kyori:adventure-nbt:4.24.0")
    compileOnly("com.maxmind.geoip2:geoip2:4.4.0")
    compileOnly("org.apache.ant:ant:1.10.15")
    compileOnly("com.zaxxer:HikariCP:7.0.2")
    compileOnly("net.luckperms:api:5.5")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("io.github.miniplaceholders:miniplaceholders-kotlin-ext:3.0.1")
    compileOnly("com.github.milkbowl:VaultAPI:1.7.1")
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.15")
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.2.2")

    testImplementation(kotlin("test"))
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    test {
        useJUnitPlatform()
    }
    runServer {
        minecraftVersion("1.21.10")
        runDirectory(file("run/paper"))
    }
    runPaper.folia.registerTask()
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

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("PunisherX")
    archiveClassifier.set("")
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
                url.set("https://github.com/SyntaxDevTeam/PunisherX")
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

plugindeployer {
    paper { dir = "/home/debian/poligon/1.21.10/Paper/plugins" }
    folia { dir = "/home/debian/poligon/1.21.10/Folia/plugins" }
}