plugins {
    kotlin("ios") version "1.9.22"
}

kotlin {
    iosXcode {
        useFramework = "shared"
        deploymentTarget = 15
    }
}
