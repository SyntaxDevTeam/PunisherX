import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.gradle.api.file.DuplicatesStrategy

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/releases/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:4.1.2-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:4.1.2-SNAPSHOT")
    implementation("com.zaxxer:HikariCP:7.1.0") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation("com.mysql:mysql-connector-j:9.7.0")
}

extensions.configure<KotlinJvmProjectExtension> {
    jvmToolchain(25)
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    filesMatching("velocity-plugin.json") {
        expand("version" to pluginVersion)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("PunisherX-Velocity-Bridge")
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
