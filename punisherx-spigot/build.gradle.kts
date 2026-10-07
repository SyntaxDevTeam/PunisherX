import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.gradle.api.file.DuplicatesStrategy

plugins {
    id("io.papermc.hangar-publish-plugin")
    id("xyz.jpenilla.run-paper")
    id("pl.syntaxdevteam.plugindeployer")
}

description = "Advanced punishment system for Spigot servers with commands like warn, mute, jail, ban, kick and more."

repositories {
    // SyntaxDevTeam
    maven("https://nexus.syntaxdevteam.pl/repository/maven-snapshots/") {
        content {
            includeGroup("pl.syntaxdevteam")
        }
    }

    maven("https://nexus.syntaxdevteam.pl/repository/maven-releases/") {
        content {
            includeGroup("pl.syntaxdevteam")
        }
    }
    // Standard dependencies
    mavenCentral()
    // AlessioDP
    maven("https://repo.alessiodp.com/releases/") {
        name = "alessioReleases"
    }

    // Spigot API
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotSnapshots"

        content {
            includeGroup("org.spigotmc")
        }
    }

    // Sonatype
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }

    // PlaceholderAPI
    maven("https://repo.extendedclip.com/releases/") {
        name = "placeholderApi"

        content {
            includeGroup("me.clip")
        }
    }

    // VaultUnlocked
    maven("https://repo.codemc.org/repository/maven-public/") {
        name = "codeMc"

        content {
            includeGroup("net.milkbowl.vault")
        }
    }

    // Legacy VaultAPI
    maven("https://jitpack.io") {
        name = "jitpack"

        content {
            includeGroup("com.github.milkbowl")
        }
    }

    // EssentialsX
    maven("https://repo.essentialsx.net/releases/") {
        name = "essentialsX"

        content {
            includeGroup("net.essentialsx")
        }
    }

    // FastStats — tylko to repo może obsługiwać FastStats
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
}

dependencies {
    compileOnly(libs.spigot.api)
    implementation(libs.libby.bukkit)

    compileOnly(libs.syntaxcore)
    compileOnly(libs.messagehandler.spigot)

    compileOnly(libs.aether.api)
    compileOnly(libs.snakeyaml)
    compileOnly(libs.gson)
    compileOnly(libs.geoip)
    compileOnly(libs.ant)
    compileOnly(libs.boosted.yaml)
    compileOnly(libs.triumph.gui)

    compileOnly(libs.caffeine)
    compileOnly(libs.adventure.nbt)
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.key)
    compileOnly(libs.adventure.platform.api)
    compileOnly(libs.adventure.platform.bukkit)
    compileOnly(libs.adventure.platform.facet)
    compileOnly(libs.adventure.minimessage)
    compileOnly(libs.adventure.json)
    compileOnly(libs.adventure.gson)
    compileOnly(libs.adventure.legacy)
    compileOnly(libs.adventure.plain)
    compileOnly(libs.adventure.ansi)
    compileOnly(libs.examination.api)
    compileOnly(libs.examination.string)
    compileOnly(libs.kyori.option)

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
    testImplementation(libs.spigot.api)
}

extensions.configure<KotlinJvmProjectExtension> {
    jvmToolchain(21)
}

val runtimeLibraryVersions = mapOf(
    "aetherVersion" to libs.versions.aether.get(),
    "snakeyamlVersion" to libs.versions.snakeyaml.get(),
    "gsonVersion" to libs.versions.gson.get(),
    "caffeineVersion" to libs.versions.caffeine.get(),
    "adventureVersion" to libs.versions.adventure.core.get(),
    "adventurePlatformVersion" to libs.versions.adventure.platform.get(),
    "examinationVersion" to libs.versions.examination.get(),
    "optionVersion" to libs.versions.kyori.option.get(),
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
        "description" to project.description
    )
    inputs.properties(props + runtimeLibraryVersions)
    filteringCharset = "UTF-8"
    filesMatching(listOf("plugin.yml")) {
        expand(props)
    }
    filesMatching("spigot-libraries.yml") {
        expand(runtimeLibraryVersions)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("PunisherX-Spigot")
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

    relocate(
        "net.byteflux.libby",
        "pl.syntaxdevteam.punisher.libs.libby"
    )

    dependencies {
        include(dependency("net.byteflux:libby-bukkit"))
        include(dependency("net.byteflux:libby-core"))
        include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        include(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7"))
        include(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8"))
    }
}

plugindeployer {
    paper { dir = "/home/debian/server/Paper/26.2/plugins" } //ostatnia wersja dla Paper
    folia { dir = "/home/debian/server/Folia/26.2/plugins" } //ostatnia wersja dla Folia
    spigot { dir = "/home/debian/server/Spigot/26.2/plugins" } //ostatnia wersja dla Spigot
}
