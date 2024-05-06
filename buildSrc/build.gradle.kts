/*
 * MIT License
 *
 * Copyright (c) 2024 Cyan Kneelawk.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

plugins {
    id("java-gradle-plugin")
    kotlin("jvm") version "1.9.20"
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://maven.architectury.dev/") { name = "Architectury" }
    maven("https://maven.quiltmc.org/repository/release") { name = "Quilt" }
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    maven("https://kneelawk.com/maven") { name = "Kneelawk" }
}

dependencies {
    val loom_version: String by project
    implementation("net.fabricmc:fabric-loom:$loom_version")

    val userdev_version: String by project
    implementation("net.neoforged.gradle:userdev:$userdev_version")
}

gradlePlugin {
    plugins {
        create("versioningPlugin") {
            id = "com.kneelawk.versioning"
            implementationClass = "com.kneelawk.versioning.VersioningPlugin"
        }
        create("submodulePlugin") {
            id = "com.kneelawk.submodule"
            implementationClass = "com.kneelawk.submodule.SubmodulePlugin"
        }
        create("kpublishPlugin") {
            id = "com.kneelawk.kpublish"
            implementationClass = "com.kneelawk.kpublish.KPublishPlugin"
        }
    }
}
