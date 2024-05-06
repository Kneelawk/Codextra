plugins {
    id("dev.architectury.loom")
    id("com.kneelawk.remapcheck")
}

remapCheck {
    val minecraft_version: String by project
    val yarn_version: String by project
    applyTargetMapping("net.fabricmc:yarn:$minecraft_version+build.$yarn_version:v2")
    checkRemap {
        targetProject(":fabric")
    }
}
