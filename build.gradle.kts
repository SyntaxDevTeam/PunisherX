import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.papermc.hangarpublishplugin.model.Platforms
import org.gradle.api.publish.maven.MavenPublication

plugins {
    kotlin("jvm") version "2.4.0-Beta1"
    id("com.gradleup.shadow") version "9.4.1"
    id("org.jetbrains.dokka-javadoc") version "2.2.0" apply false
    `maven-publish`
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("pl.syntaxdevteam.plugindeployer") version "1.0.5-R0.1-SNAPSHOT"
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
    version = bridgeVersion
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

repositories {
    maven("https://nexus.syntaxdevteam.pl/repository/maven-snapshots/") //SyntaxDevTeam
    maven("https://nexus.syntaxdevteam.pl/repository/maven-releases/") //SyntaxDevTeam
    gradlePluginPortal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://repo.menthamc.org/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/") // PlaceholderAPI
    maven("https://repo.codemc.org/repository/maven-public/") // VaultUnlockedAPI
    maven("https://jitpack.io") // VaultAPI
    maven("https://repo.essentialsx.net/releases/") // EssentialsX
    maven("https://repo.leavesmc.org/snapshots/") {
        name = "leavesmc-repo"
    }
    maven("https://repo.faststats.dev/releases") {
        name = "faststatsReleases"
    }
}

val mockitoAgent: Configuration by configurations.creating

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    //compileOnly("org.leavesmc.leaves:leaves-api:1.21.10-R0.1-SNAPSHOT")
    //compileOnly("dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT")
    //compileOnly("me.earthme.luminol:luminol-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("pl.syntaxdevteam:syntaxcore:1.3.0")
    compileOnly("pl.syntaxdevteam:messageHandler-paper:1.2.0-R0.2-SNAPSHOT")
    compileOnly("pl.syntaxdevteam:syntaxgui-api:0.1.0-R0.1-SNAPSHOT")

    compileOnly("org.eclipse.aether:aether-api:1.1.0")
    compileOnly("org.yaml:snakeyaml:2.6")
    compileOnly("com.google.code.gson:gson:2.13.2")
    compileOnly("com.maxmind.geoip2:geoip2:5.0.2")
    compileOnly("org.apache.ant:ant:1.10.17")
    compileOnly("com.zaxxer:HikariCP:7.0.2")
    compileOnly("net.luckperms:api:5.5")
    compileOnly("me.clip:placeholderapi:2.12.2")
    compileOnly("io.github.miniplaceholders:miniplaceholders-kotlin-ext:3.1.0")
    compileOnly("com.github.milkbowl:VaultAPI:1.7.1")
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.15")
    compileOnly("net.essentialsx:EssentialsXSpawn:2.21.2"){
        isTransitive = false
    }
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.2.3")
    compileOnly("dev.dejvokep:boosted-yaml:1.3.7")
    compileOnly("pl.syntaxdevteam:DscBridgeAPI:1.0.0-R0.7-SNAPSHOT")

    compileOnly("dev.faststats.metrics:bukkit:0.22.0")

    testImplementation(kotlin("test"))
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
    mockitoAgent("net.bytebuddy:byte-buddy-agent:1.18.8") {
        isTransitive = false
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    test {
        useJUnitPlatform()
        jvmArgs("-javaagent:${mockitoAgent.singleFile}")
    }
    runServer {
        minecraftVersion("26.1.1")
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

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("PunisherX")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    mergeServiceFiles()
    dependencies {
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-reflect"))
        exclude(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core"))
        exclude(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8"))
    }
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
            name = "NexusSnapshots"
            url = uri("https://nexus.syntaxdevteam.pl/repository/maven-snapshots/")
            credentials {
                username = findProperty("nexusUser")?.toString()
                password = findProperty("nexusPassword")?.toString()
            }
            mavenContent { snapshotsOnly() }
        }
        maven {
            name = "NexusReleases"
            url = uri("https://nexus.syntaxdevteam.pl/repository/maven-releases/")
            credentials {
                username = findProperty("nexusUser")?.toString()
                password = findProperty("nexusPassword")?.toString()
            }
            mavenContent { releasesOnly() }
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
    paper { dir = "/home/debian/server/Paper/26.1.2/plugins" } //ostatnia wersja dla Paper
    folia { dir = "/home/debian/server/Folia/1.21.11/plugins" } //ostatnia wersja dla Folia
}
