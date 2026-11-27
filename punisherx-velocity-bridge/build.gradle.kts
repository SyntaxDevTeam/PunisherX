import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.gradle.api.file.DuplicatesStrategy

repositories {
    maven("https://nexus.syntaxdevteam.pl/repository/maven-snapshots/")
    maven("https://nexus.syntaxdevteam.pl/repository/maven-releases/")
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/releases/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.alessiodp.com/releases/") {
        content { includeGroup("net.byteflux") }
    }
}

// Libby loads these artifacts individually and does not resolve transitive POMs.
val databaseLibraries = listOf(libs.syntaxdb.core, libs.hikari, libs.mysql.jdbc, libs.protobuf)

dependencies {
    compileOnly(libs.velocity.api)
    compileOnly(libs.syntaxdb.core)
    implementation(libs.libby.velocity)
    databaseLibraries.forEach { testImplementation(it) }
    testRuntimeOnly(libs.sqlite.jdbc)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.velocity.api)
}

tasks.test { useJUnitPlatform() }

extensions.configure<KotlinJvmProjectExtension> {
    jvmToolchain(25)
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    val databaseCoordinates = databaseLibraries.joinToString("\n") { it.get().toString() }
    inputs.property("pluginVersion", pluginVersion)
    inputs.property("databaseCoordinates", databaseCoordinates)
    filesMatching("velocity-plugin.json") {
        expand("version" to pluginVersion)
    }
    filesMatching("velocity-libraries.txt") {
        expand("databaseCoordinates" to databaseCoordinates)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("PunisherX-Velocity-Bridge")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    relocate("net.byteflux.libby", "pl.syntaxdevteam.punisher.bridge.velocity.libs.libby")

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
