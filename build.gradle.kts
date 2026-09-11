plugins {
    `java-library`
    `maven-publish`
    idea
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
}

group = "de.mcjj.siedler"
version = "2.0.0-SNAPSHOT"
description = "Siedler 2.0 - PowerNukkitX plugin"

java.sourceCompatibility = JavaVersion.VERSION_21

// PowerNukkitX 3.0.4 targets Minecraft Bedrock 1.26.45.
dependencies {
    compileOnly("com.github.PowerNukkitX:PowerNukkitX:master-SNAPSHOT")
}

java {
    withSourcesJar()
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = false
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xpkginfo:always")
    java.sourceCompatibility = JavaVersion.VERSION_21
    java.targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<AbstractCopyTask> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
