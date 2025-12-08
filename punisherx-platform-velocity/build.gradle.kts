plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":punisherx-api"))
    implementation(project(":punisherx-core"))
    implementation(project(":punisherx-compat"))
}

java {
    withSourcesJar()
}
