import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidApplication)
}

val oidcWebClientId: String =
    (findProperty("fixaverse.oidc.webClientId") as String?)
        ?: System.getenv("FIXAVERSE_OIDC_WEB_CLIENT_ID")
        ?: "unset-web-client-id"

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

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
            implementation(compose.components.resources)

            implementation(projects.auth)
            implementation(projects.shared)
            implementation(projects.designSystem)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.decompose)
            implementation(libs.decompose.compose)
            implementation(libs.essenty.lifecycle)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.vico.compose)
            implementation(libs.vico.compose.m3)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.rustore.appupdate)
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

android {
    namespace = "pro.masterdoc.client"
    compileSdk = 36

    defaultConfig {
        applicationId = "pro.masterdoc.client"
        minSdk = 26
        targetSdk = 36
        versionCode = (findProperty("VERSION_CODE") as String?)?.toIntOrNull() ?: 10000
        versionName = findProperty("VERSION_NAME") as String? ?: "1.0.0"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD").orEmpty()
                keyAlias = System.getenv("ANDROID_KEY_ALIAS").orEmpty()
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD").orEmpty()
            }
        }
    }

    buildTypes {
        debug {}
        release {
            isMinifyEnabled = false
            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

compose.desktop {
    application {
        mainClass = "pro.masterdoc.client.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "client-app"
            packageVersion = "1.0.0"
        }
    }
}

tasks.register("generateAuthDefaults") {
    val outputDir = layout.buildDirectory.dir("generated/authDefaults/kotlin")
    inputs.property("oidcWebClientId", oidcWebClientId)
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.resolve("pro/masterdoc/client")
        dir.mkdirs()
        dir.resolve("GeneratedAuthDefaults.kt").writeText(
            """
            package pro.masterdoc.client

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

dependencies {
    debugImplementation(compose.uiTooling)
}
