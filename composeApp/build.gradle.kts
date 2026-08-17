plugins {
    `kotlin-multiplatform`
}

kotlin {
    matrix {
        withComposeWasmWeb("webApp") {
            compose {
                enablePreview()
            }
        }
    }

    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Shared compose dependencies
            }
        }
        val commonTest by getting {
            dependencies {
                kotest("assertions")
                kotest("junit5")
            }
        }
        val webMain by getting {
            dependsOn(commonMain)
        }
        val webTest by getting {
            dependsOn(commonTest)
        }
    }
}
