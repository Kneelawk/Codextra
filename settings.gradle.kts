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
}

rootProject.name = "codextra"
