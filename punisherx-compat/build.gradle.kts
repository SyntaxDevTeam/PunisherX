plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":punisherx-api"))
    implementation(project(":punisherx-core"))
}

java {
    withSourcesJar()
}
