import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/releases/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.md-5.net/content/groups/public/")
    maven("https://libraries.minecraft.net")
}

dependencies {
    compileOnly("net.md-5:bungeecord-api:1.21-R0.4")
}

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("net.md-5:brigadier")).using(module("com.mojang:brigadier:1.0.500"))
    }
}

extensions.configure<KotlinJvmProjectExtension> {
    jvmToolchain(21)
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    filesMatching("bungee.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("PunisherX-BungeeCord-Bridge")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}
