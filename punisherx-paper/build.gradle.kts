@file:Suppress("AvoidDuplicateDependencies")

import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.papermc.hangarpublishplugin.model.Platforms
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.file.DuplicatesStrategy

plugins {
    id("io.papermc.hangar-publish-plugin")
    id("xyz.jpenilla.run-paper")
    id("pl.syntaxdevteam.plugindeployer")
}

group = "pl.syntaxdevteam.punisher"
description = "Advanced punishment system for Minecraft servers with commands like warn, mute, jail, ban, kick and more."

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

val serverJavaLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    // Opt-in for testing a locally published MessageHandler before uploading its snapshot.
    if (providers.gradleProperty("punisherx.messagehandler.local").orNull == "true") {
        mavenLocal {
            content {
                includeModule("pl.syntaxdevteam", "messageHandler-paper")
                includeModule("pl.syntaxdevteam", "messageHandler-spigot")
            }
        }
    }
    maven("https://nexus.syntaxdevteam.pl/repository/maven-snapshots/")
    maven("https://nexus.syntaxdevteam.pl/repository/maven-releases/")

    gradlePluginPortal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://repo.extendedclip.com/releases/") {
        content {
            includeGroup("me.clip")
        }
    }
    maven("https://repo.codemc.org/repository/maven-public/") {
        content {
            includeGroup("net.milkbowl.vault")
        }
    }
    maven("https://jitpack.io") {
        content {
            includeGroup("com.github.milkbowl")
        }
    }
    maven("https://repo.essentialsx.net/releases/") {
        content {
            includeGroup("net.essentialsx")
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "faststatsReleases"
                url = uri("https://repo.faststats.dev/releases")
            }
        }

        filter {
            includeGroup("dev.faststats.metrics")
        }
    }
    maven("https://repo.leavesmc.org/snapshots/") {
        name = "leavesmc-repo"

        content {
            includeGroup("org.leavesmc.leaves")
        }
    }
}

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.syntaxdb.core) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    compileOnly(libs.syntaxcore)
    compileOnly(libs.messagehandler.paper)

    compileOnly(libs.aether.api)
    compileOnly(libs.snakeyaml)
    compileOnly(libs.gson)
    compileOnly(libs.geoip)
    compileOnly(libs.ant)
    compileOnly(libs.caffeine)
    compileOnly(libs.boosted.yaml)
    compileOnly(libs.triumph.gui)

    compileOnly(libs.adventure.nbt)

    compileOnly(libs.luckperms.api)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.miniplaceholders.kotlin)
    compileOnly(libs.vault.api)
    compileOnly(libs.vault.unlocked.api)
    compileOnly(libs.essentialsx.spawn) {
        isTransitive = false
    }
    compileOnly(libs.dscbridge.api)

    compileOnly(libs.faststats.bukkit)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.syntaxdb)
    testImplementation(libs.syntaxcore) {
        isTransitive = false
    }
    testImplementation(libs.sqlite.jdbc)
    testImplementation(libs.paper.api)
    testImplementation(libs.messagehandler.paper)
    testImplementation(libs.caffeine)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)
    mockitoAgent(libs.byte.buddy.agent) {
        isTransitive = false
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    test {
        // SyntaxCore bundles Adventure classes; use the Paper API's versions in tests.
        val syntaxCoreJars = classpath.filter { it.name.startsWith("syntaxcore-") }
        classpath = files(classpath - syntaxCoreJars, syntaxCoreJars)
        useJUnitPlatform()
        jvmArgs("-javaagent:${mockitoAgent.singleFile}")
    }
    runServer {
        javaLauncher.set(serverJavaLauncher)
        minecraftVersion(libs.versions.paper.run.get())
        runDirectory(rootProject.file("run/paper"))
    }
    runPaper.folia.registerTask {
        javaLauncher.set(serverJavaLauncher)
        minecraftVersion(libs.versions.folia.run.get())
        runDirectory(rootProject.file("run/folia"))
    }
}

val runtimeLibraryVersions = mapOf(
    "kotlinVersion" to libs.versions.kotlin.get(),
    "syntaxdbVersion" to libs.versions.syntaxdb.get(),
    "hikariVersion" to libs.versions.hikari.get(),
    "mysqlVersion" to libs.versions.mysql.get(),
    "postgresqlVersion" to libs.versions.postgresql.get(),
    "mssqlVersion" to libs.versions.mssql.get(),
    "sqliteVersion" to libs.versions.sqlite.get(),
    "protobufVersion" to libs.versions.protobuf.get(),
    "aetherVersion" to libs.versions.aether.get(),
    "snakeyamlVersion" to libs.versions.snakeyaml.get(),
    "gsonVersion" to libs.versions.gson.get(),
    "caffeineVersion" to libs.versions.caffeine.get(),
    "adventureVersion" to libs.versions.adventure.core.get(),
    "triumphGuiVersion" to libs.versions.triumph.gui.get(),
    "geoipVersion" to libs.versions.geoip.get(),
    "antVersion" to libs.versions.ant.get(),
    "faststatsVersion" to libs.versions.faststats.get(),
    "syntaxcoreVersion" to libs.versions.syntaxcore.get(),
    "messagehandlerVersion" to libs.versions.messagehandler.get(),
)

tasks.processResources {
    val props = mapOf(
        "version" to version,
        "description" to description
    )
    inputs.properties(props + runtimeLibraryVersions)
    filteringCharset = "UTF-8"
    filesMatching(listOf("paper-plugin.yml")) {
        expand(props)
    }
    filesMatching("paper-libraries.yml") {
        expand(runtimeLibraryVersions)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("PunisherX-Paper")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    mergeServiceFiles()

    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    filesMatching("META-INF/*.kotlin_module") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    dependencies {
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8"))
    }
}


publishing {
    publications {
        create<MavenPublication>("punisherx") {
            artifact(tasks.named("shadowJar").get()) {
                classifier = null
            }
            pom {
                name.set("punisherx-api")
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
            val releasesRepoUrl = uri("https://nexus.syntaxdevteam.pl/repository/maven-releases/")
            val snapshotsRepoUrl = uri("https://nexus.syntaxdevteam.pl/repository/maven-snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = findProperty("nexusUser")
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: System.getenv("NEXUS_USER")
                        ?.takeIf { it.isNotBlank() }

                password = findProperty("nexusPassword")
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: System.getenv("NEXUS_PASSWORD")
                        ?.takeIf { it.isNotBlank() }
            }
        }
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    if (repository.name == "Nexus") {
        doFirst {
            val nexusCredentials = repository.credentials
            if (nexusCredentials.username.isNullOrBlank() || nexusCredentials.password.isNullOrBlank()) {
                throw GradleException(
                    "Brak danych do publikacji w Nexus: ustaw 'nexusUser' i 'nexusPassword' " +
                        "w gradle.properties lub zmienne srodowiskowe NEXUS_USER i NEXUS_PASSWORD"
                )
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
        changelog.set(rootProject.file("CHANGELOG.md").readText())
    }
}

plugindeployer {
    paper { dir = "/home/debian/server/Paper/26.2/plugins" } //ostatnia wersja dla Paper
    folia { dir = "/home/debian/server/Folia/26.1.2/plugins" } //ostatnia wersja dla Folia
    spigot { dir = "/home/debian/server/Spigot/26.2/plugins" } //ostatnia wersja dla Spigot
}

// Keep shared Paper classes ahead of SyntaxCore's bundled classes when compiling tests.
tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin") {
    val testLibraries = libraries.files
    val syntaxCoreJars = testLibraries.filter { it.name.startsWith("syntaxcore-") }
    libraries.setFrom(testLibraries - syntaxCoreJars.toSet(), syntaxCoreJars)
}
