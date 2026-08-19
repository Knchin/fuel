plugins {
    kotlin("js")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

tasks.register<Copy>("copyIndexHtml") {
    dependsOn("browserProductionWebpack")
    from("src/main/resources/index.html")
    into("build/distributions")
}

tasks.named("browserProductionWebpack") {
    finalizedBy("copyIndexHtml")
}
