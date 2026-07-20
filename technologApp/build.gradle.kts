import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

val oidcWebClientId: String =
    (findProperty("fixaverse.oidc.webClientId") as String?)
        ?: System.getenv("FIXAVERSE_OIDC_WEB_CLIENT_ID")
        ?: "unset-web-client-id"

kotlin {
    applyDefaultHierarchyTemplate()

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)

            implementation(projects.auth)
            implementation(projects.shared)
            implementation(projects.designSystem)
            implementation(libs.decompose)
            implementation(libs.decompose.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }

        val wasmJsMain by getting
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
    }
}

compose.desktop {
    application {
        mainClass = "pro.masterdoc.client.technolog.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "fixaverse-technolog"
            packageVersion = "1.0.0"
        }
    }
}

tasks.register("generateAuthDefaults") {
    val outputDir = layout.buildDirectory.dir("generated/authDefaults/kotlin")
    inputs.property("oidcWebClientId", oidcWebClientId)
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.resolve("pro/masterdoc/client/technolog")
        dir.mkdirs()
        dir.resolve("GeneratedAuthDefaults.kt").writeText(
            """
            package pro.masterdoc.client.technolog

            internal object GeneratedAuthDefaults {
                const val WEB_CLIENT_ID: String = "$oidcWebClientId"
            }
            """.trimIndent() + "\n",
        )
    }
}

kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(
    layout.buildDirectory.dir("generated/authDefaults/kotlin"),
)

tasks.matching { it.name.startsWith("compile") }.configureEach {
    dependsOn("generateAuthDefaults")
}
