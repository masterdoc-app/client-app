rootProject.name = "client-app"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Prefer sibling / CI checkout of fixaverse-design — GitHub Packages needs read:packages.
val fixaverseDesignDir =
    sequenceOf(
        rootDir.resolve("fixaverse-design"),
        rootDir.resolve("../fixaverse-design"),
    ).firstOrNull { it.isDirectory && it.resolve("settings.gradle.kts").isFile }

if (fixaverseDesignDir != null) {
    includeBuild(fixaverseDesignDir) {
        dependencySubstitution {
            substitute(module("pro.fixaverse:design-theme")).using(project(":theme"))
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven {
            url = uri("https://artifactory-external.vkpartner.ru/artifactory/maven")
        }
        val gprUser =
            providers.gradleProperty("gpr.user").orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
        val gprKey =
            providers.gradleProperty("gpr.key").orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
        if (!gprKey.isNullOrBlank()) {
            maven {
                url = uri("https://maven.pkg.github.com/masterdoc-app/fixaverse-design")
                credentials {
                    username = gprUser ?: "token"
                    password = gprKey
                }
            }
        }
    }
}

include(":design-system")
include(":auth")
include(":shared")
include(":composeApp")
