import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/releases/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("com.mysql:mysql-connector-j:9.6.0")
}

extensions.configure<KotlinJvmProjectExtension> {
    jvmToolchain(21)
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
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}
