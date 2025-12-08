plugins {
    kotlin("jvm") version "2.3.0-RC2" apply false
    id("com.gradleup.shadow") version "9.3.0" apply false
    id("io.papermc.hangar-publish-plugin") version "0.1.3" apply false
    id("xyz.jpenilla.run-paper") version "3.0.2" apply false
    id("pl.syntaxdevteam.plugindeployer") version "1.0.4" apply false
    `maven-publish`
}

allprojects {
    group = "pl.syntaxdevteam.punisher"
    version = "1.7.0-DEV"
}

subprojects {
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
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure(org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension::class) {
            jvmToolchain(21)
        }
    }
}
