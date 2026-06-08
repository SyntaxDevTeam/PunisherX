import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotSnapshots"
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:26.1.2-R0.1-SNAPSHOT")
    testImplementation(kotlin("test"))
}

extensions.configure<KotlinJvmProjectExtension> {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
