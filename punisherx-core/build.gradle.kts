plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":punisherx-api"))
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.2.3")
}

java {
    withSourcesJar()
}
