plugins {
    `java-library`
    `maven-publish`
    idea
}

repositories {
    mavenLocal()
    maven("https://repo.maven.apache.org/maven2/")
    maven("https://jitpack.io")
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
}

group = "de.jawollo07.siedler"
version = "2.0.0-SNAPSHOT"
description = "Siedler 2.0 - Native PowerNukkitX plugin"
java.sourceCompatibility = JavaVersion.VERSION_21

dependencies {
    compileOnly(files("powernukkitx.jar"))
}

//Automatically download dependencies source code
idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = false
    }
}

java {
    withSourcesJar()
}

sourceSets {
    main {
        resources {
            srcDirs("src/main/resources")
        }
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xpkginfo:always")
    java.sourceCompatibility = JavaVersion.VERSION_21
    java.targetCompatibility = JavaVersion.VERSION_21
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(listOf("--add-opens", "java.base/java.lang=ALL-UNNAMED"))
    jvmArgs(listOf("--add-opens", "java.base/java.io=ALL-UNNAMED"))
}

tasks.withType<AbstractCopyTask>() {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
