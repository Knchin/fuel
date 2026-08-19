rootProject.name = "fuel-station-comparison"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    plugins {
        // Explicitly manage Kotlin plugin versions
        id("org.jetbrains.kotlin.multiplatform") version "1.8.22"
        id("org.jetbrains.kotlin.android") version "1.8.22"
        id("org.jetbrains.kotlin.jvm") version "1.8.22"
        id("org.jetbrains.kotlin.js") version "1.8.22"
    }
}

include("androidApp")
include("iosApp")
include("composeApp")
include("shared")
include("webApp")
include("backend")
include("cloudflare")
