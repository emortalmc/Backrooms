import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    java
}

group = "dev.emortal"
version = "1.0"

repositories {
    mavenCentral()

    maven(url = "https://repo.spongepowered.org/maven")
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("com.github.Minestom:Minestom:c32153d221")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        manifest {
            attributes (
                "Main-Class" to "dev.emortal.backrooms.BackroomsMainKt",
                "Multi-Release" to true
            )
        }

        mergeServiceFiles()

        archiveBaseName.set("Backrooms")
    }

    build { dependsOn(shadowJar) }

    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

}
