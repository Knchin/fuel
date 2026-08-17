plugins {
    `kotlin-wasm-js`
}

kotlin {
    wasmJs {
        binaries["wasmJs"] {
            // ...
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Shared dependencies
            }
        }
        val webMain by getting {
            dependsOn(commonMain)
        }
    }
}
