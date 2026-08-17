plugins {
    `kotlin-multiplatform`
}

kotlin {
    matrix {
        withAndroid("androidApp") {
            compileKotlin("androidApp")
            compileKotlinKotlin("androidApp")
            run("androidApp")
        }
        withIOSNative("iosApp") {
            compileKotlin("iosApp")
        }
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
                implementation("france.fuel.station:shared-domain:1.0.0")
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-client-json:2.3.7")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            }
        }

        val commonTest by getting {
            dependencies {
                kotest("assertions")
                kotest("junit5")
            }
        }

        val androidMain by getting {
            dependsOn(commonMain)
        }

        val androidTest by getting {
            dependsOn(commonTest)
        }

        val iosMain by getting {
            dependsOn(commonMain)
        }

        val iosTest by getting {
            dependsOn(commonTest)
        }

        val jsMain by getting {
            dependsOn(commonMain)
        }

        val jsTest by getting {
            dependsOn(commonTest)
        }
    }
}
