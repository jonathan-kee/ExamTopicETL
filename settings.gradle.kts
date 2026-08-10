plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "static_page"
include("Download")
include("Document")
include("Image")
include("KotlinMain")
include("KotlinDownload")
include("KotlinDocument")
include("KotlinImage")
