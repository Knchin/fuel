plugins {
    kotlin("js")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
}

tasks.register<Copy>("copyIndexHtml") {
    dependsOn("browserProductionWebpack")
    from("src/main/resources/index.html")
    into("build/distributions")
}

tasks.named("browserProductionWebpack") {
    finalizedBy("copyIndexHtml")
}
