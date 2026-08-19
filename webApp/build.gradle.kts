plugins {
    kotlin("js")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Shared dependencies
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
        }
    }
}
