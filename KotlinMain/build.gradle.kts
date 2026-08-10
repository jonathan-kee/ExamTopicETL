plugins {
    // Kotlin JVM plugin replacing the standard java plugin
    kotlin("jvm") version "1.9.23"
    // Apply the Shadow plugin
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

allprojects {
    // Apply Kotlin JVM and Shadow across all subprojects
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories {
        mavenCentral()
        flatDir {
            // Points to the root project's build/libs folder
            dirs(rootProject.layout.buildDirectory.dir("libs"))
        }
    }

    dependencies {
        // Kotlin standard library (required for Kotlin projects)
        implementation(kotlin("stdlib"))

        // Standard dependencies
        testImplementation(platform("org.junit:junit-bom:5.10.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        implementation("org.jsoup:jsoup:1.22.2")
        implementation("org.postgresql:postgresql:42.7.13")
    }

    tasks.shadowJar {
        archiveClassifier.set("all")
        manifest {
            attributes(
                // Note: If your main Kotlin file is Main.kt with a top-level main function,
                // Kotlin compiles it to MainKt by default unless annotated with @file:JvmName("Main")
                "Main-Class" to "com.jonathankee.MainKt"
            )
        }
    }

    tasks.test {
        useJUnitPlatform()
    }
}