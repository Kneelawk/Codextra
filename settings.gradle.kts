pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.architectury.dev/") { name = "Architectury" }
        maven("https://maven.quiltmc.org/repository/release") { name = "Quilt" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://kneelawk.com/maven") { name = "Kneelawk" }
    }
    plugins {
        val architectury_loom_version: String by settings
        id("dev.architectury.loom") version architectury_loom_version
        val remapcheck_version: String by settings
        id("com.kneelawk.remapcheck") version remapcheck_version
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "codextra"

include(":xplat")
include(":xplat-mojmap")
include(":fabric", ":fabric:remapCheck")
//include(":neoforge")
