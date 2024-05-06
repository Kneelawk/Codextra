/*
 * MIT License
 *
 * Copyright (c) 2023 Kneelawk.
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

package com.kneelawk.submodule

import com.kneelawk.getProperty
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

class SubmodulePlugin : Plugin<Project> {
    private val metadataFiles = listOf(
        "quilt.mod.json",
        "fabric.mod.json",
        "META-INF/mods.toml",
        "META-INF/neoforge.mods.toml",
        "pack.mcmeta"
    )

    override fun apply(project: Project) {
        project.plugins.apply("dev.architectury.loom")

        val baseEx = project.extensions.getByType(BasePluginExtension::class)
        val javaEx = project.extensions.getByType(JavaPluginExtension::class)
        val loomEx = project.extensions.getByType(LoomGradleExtensionAPI::class)

        project.extensions.create("submodule", SubmoduleExtension::class, project)

        val mavenGroup = project.getProperty<String>("maven_group")
        project.group = mavenGroup
        val archivesBaseName = project.getProperty<String>("archives_base_name")
        baseEx.archivesName.set("${archivesBaseName}-${project.name}")

        val javaVersion = if (System.getenv("JAVA_VERSION") != null) {
            System.getenv("JAVA_VERSION")
        } else {
            project.getProperty<String>("java_version")
        }

        javaEx.apply {
            sourceCompatibility = JavaVersion.toVersion(javaVersion)
            targetCompatibility = JavaVersion.toVersion(javaVersion)

            toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))

            withSourcesJar()
        }

        project.repositories.apply {
            mavenCentral()
            maven("https://maven.quiltmc.org/repository/release") { name = "Quilt" }
            maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
            maven("https://maven.firstdark.dev/snapshots") { name = "FirstDark" }
            maven("https://kneelawk.com/maven") { name = "Kneelawk" }
            maven("https://maven.alexiil.uk/") { name = "AlexIIL" }
            maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }

            mavenLocal()
        }

        project.dependencies.apply {
            val minecraftVersion = project.getProperty<String>("minecraft_version")
            add("minecraft", "com.mojang:minecraft:$minecraftVersion")
            val parchmentVersion = project.getProperty<String>("parchment_version")
            add("mappings", loomEx.layered {
                officialMojangMappings()
                parchment("org.parchmentmc.data:parchment-$minecraftVersion:$parchmentVersion@zip")
            })

            add("testImplementation", "junit:junit:4.13.2")
        }

        project.tasks.apply {
            named("processResources", ProcessResources::class.java).configure {
                val properties = mapOf(
                    "version" to project.version
                )

                inputs.properties(properties)

                filesMatching(metadataFiles) {
                    expand(properties)
                }
            }

            withType<JavaCompile>().configureEach {
                options.encoding = "UTF-8"
                options.release.set(javaVersion.toInt())
            }

            named("jar", Jar::class).configure {
                from(project.rootProject.file("LICENSE")) {
                    rename { "${it}_${project.rootProject.name}" }
                }
                archiveClassifier.set("")
            }

            named("sourcesJar", Jar::class).configure {
                from(project.rootProject.file("LICENSE")) {
                    rename { "${it}_${project.rootProject.name}" }
                }
            }

            named("javadoc", Javadoc::class).configure {
                exclude("com/kneelawk/codextra/impl")
                exclude("com/kneelawk/codextra/**/impl")

                val jetbrainsAnnotationsVersion = project.getProperty<String>("jetbrains_annotations_version")
                (options as? StandardJavadocDocletOptions)?.links = listOf(
                    "https://javadoc.io/doc/org.jetbrains/annotations/${jetbrainsAnnotationsVersion}/"
                )

                options.optionFiles(project.rootProject.file("javadoc-options.txt"))
            }

            named("test", Test::class.java).configure {
                useJUnit()
            }
        }

        project.afterEvaluate {
            tasks.findByName("genSources")?.apply { setDependsOn(listOf("genSourcesWithVineflower")) }
        }
    }
}
