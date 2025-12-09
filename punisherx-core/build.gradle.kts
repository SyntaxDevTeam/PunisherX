plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":punisherx-api"))
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.3")
    testImplementation(kotlin("test"))
}

java {
    withSourcesJar()
}
