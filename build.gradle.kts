plugins {
    id("java")
}

group = "com.postofficer"
version = "1.6"
description = "Post Officer Plugin for PaperMC"

repositories {
    mavenCentral() // For JDK dependencies
    maven ("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    implementation ("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT") // Paper API dependency
    testImplementation ("junit:junit:4.13.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // Oracle JDK 21
    }
}

tasks {
    processResources {
        from(sourceSets.main.get().resources.srcDirs) {
            filesMatching("plugin.yml") {
                expand(
                    "name" to rootProject.name,
                    "version" to project.version
                )
            }
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }

    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21) // Ensure Java 21 compatibility for the compilation
    }
}