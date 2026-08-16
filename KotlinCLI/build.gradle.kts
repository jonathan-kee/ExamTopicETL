plugins {
    kotlin("jvm") version "2.1.0"
    // 1. Add the shadow plugin to build a "Fat JAR" containing your dependencies
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Note: I removed implementation(project(":")) as it creates an invalid self-reference
}

// 2. Tell the JAR exactly where your main function lives
tasks.withType<Jar> {
    manifest {
        // If your package is org.example and your file is Main.kt, Kotlin names the class MainKt
        attributes["Main-Class"] = "org.example.MainKt"
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}