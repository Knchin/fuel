plugins {
    kotlin("ios") version "1.8.22"
}

kotlin {
    iosXcode {
        useFramework = "shared"
        deploymentTarget = 15
    }
}
