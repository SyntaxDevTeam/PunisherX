import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.gradle.api.file.DuplicatesStrategy

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/releases/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.md-5.net/content/groups/public/")
    maven("https://libraries.minecraft.net")
}

dependencies {
    compileOnly(libs.bungeecord.api)
    implementation(libs.hikari)
    implementation(libs.mysql.connector)
}

val brigadierCoordinates = libs.brigadier.get().toString()

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("net.md-5:brigadier")).using(module(brigadierCoordinates))
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

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    mergeServiceFiles()

    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    filesMatching("META-INF/*.kotlin_module") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}
