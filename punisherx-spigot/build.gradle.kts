import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("io.papermc.hangar-publish-plugin")
    id("xyz.jpenilla.run-paper")
    id("pl.syntaxdevteam.plugindeployer")
}

description = "Advanced punishment system for Spigot servers with commands like warn, mute, jail, ban, kick and more."

repositories {
    maven("https://nexus.syntaxdevteam.pl/repository/maven-snapshots/")
    maven("https://nexus.syntaxdevteam.pl/repository/maven-releases/")
    mavenCentral()
    maven("https://repo.alessiodp.com/releases/") {
        name = "alessioReleases"
    }
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotSnapshots"
    }
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.menthamc.org/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://repo.faststats.dev/releases")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:26.1.2-R0.1-SNAPSHOT")
    implementation("net.byteflux:libby-bukkit:1.3.1")

    compileOnly("pl.syntaxdevteam:syntaxcore:1.4.0-R0.1-SNAPSHOT")
    compileOnly("pl.syntaxdevteam:messageHandler-spigot:1.2.0-R0.3-SNAPSHOT")

    compileOnly("org.eclipse.aether:aether-api:1.1.0")
    compileOnly("org.yaml:snakeyaml:2.6")
    compileOnly("com.google.code.gson:gson:2.14.0")
    compileOnly("com.maxmind.geoip2:geoip2:5.1.0")
    compileOnly("org.apache.ant:ant:1.10.17")

    compileOnly("com.github.ben-manes.caffeine:caffeine:3.2.4")
    compileOnly("dev.dejvokep:boosted-yaml:1.3.7")

    compileOnly("net.luckperms:api:5.5")
    compileOnly("me.clip:placeholderapi:2.12.2")
    compileOnly("io.github.miniplaceholders:miniplaceholders-kotlin-ext:3.1.0")
    compileOnly("com.github.milkbowl:VaultAPI:1.7.1")
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.15")
    compileOnly("net.essentialsx:EssentialsXSpawn:2.21.2") {
        isTransitive = false
    }
    compileOnly("pl.syntaxdevteam:DscBridgeAPI:1.0.0-R0.7-SNAPSHOT")
    compileOnly("dev.faststats.metrics:bukkit:0.26.1")

    testImplementation(kotlin("test"))
}

extensions.configure<KotlinJvmProjectExtension> {
    jvmToolchain(21)
}

tasks.processResources {
    val props = mapOf(
        "version" to version,
        "description" to project.description
    )
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching(listOf("plugin.yml")) {
        expand(props)
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
    relocate("net.byteflux.libby", "pl.syntaxdevteam.punisher.libs.libby")
    dependencies {
        include(dependency("net.byteflux:libby-bukkit"))
        include(dependency("net.byteflux:libby-core"))
        include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        include(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7"))
        include(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8"))
    }
}
plugindeployer {
    paper { dir = "/home/debian/server/Paper/26.1.2/plugins" } //ostatnia wersja dla Paper
    folia { dir = "/home/debian/server/Folia/26.1.2/plugins" } //ostatnia wersja dla Folia
    spigot { dir = "/home/debian/server/Spigot/26.1.2/plugins" } //ostatnia wersja dla Spigot
}
