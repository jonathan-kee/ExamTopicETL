plugins {
    id("java")
    // Apply the Shadow plugin
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

allprojects {
    apply(plugin = "java")
    repositories {
        mavenCentral()
    }
    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.10.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        implementation("org.jsoup:jsoup:1.22.2")
        implementation("org.postgresql:postgresql:42.7.13")
    }

    tasks.shadowJar {
        archiveClassifier.set("all") // Optional: names the file static_page-1.0-SNAPSHOT-all.jar
        manifest {
            attributes(
                "Main-Class" to "org.example.Main"
            )
        }
    }

    tasks.test {
        useJUnitPlatform()
    }
}