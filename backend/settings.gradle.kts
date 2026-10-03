plugins {
    // lets Gradle download the JDK requested by the toolchain when it is not installed
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "library-backend"
