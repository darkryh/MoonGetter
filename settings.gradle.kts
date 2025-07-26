pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}


rootProject.name = "MoonGetter"
include(":composeApp")
include(":androidApp")
include(":Media3Player")

include(
    ":moongetter-client-okhttp"
)

include(":moongetter-client-cookie-java-net")

include(":moongetter-client-trustmanager-java-net")

include(":moongetter-android-robot")

include(
    ":moongetter-core",
    ":moongetter-client",
    ":moongetter-jsunpacker",
    ":moongetter-client-ktor",
    ":moongetter-client-trustmanager",
    ":moongetter-core-robot"
)

include(":moongetter-doodstream")
include(":moongetter-facebook")
include(":moongetter-filemoon")
include(":moongetter-fireload")
include(":moongetter-goodstream")
include(":moongetter-googledrive")
include(":moongetter-hexload")
include(":moongetter-lamovie")
include(":moongetter-lulustream")
include(":moongetter-mediafire")
include(":moongetter-mixdrop")
include(":moongetter-mp4upload")
include(":moongetter-okru")
include(":moongetter-onecloudfile")
include(":moongetter-pixeldrain")
include(":moongetter-senvid")
include(":moongetter-streamtape")
include(":moongetter-streamwish")
include(":moongetter-uqload")
include(":moongetter-vidguard")
include(":moongetter-vihide")
include(":moongetter-vk")
include(":moongetter-voe")
include(":moongetter-xtwitter")
include(":moongetter-yourupload")

include(":moongetter-client-cookie-managment")

project(":moongetter-client-okhttp").projectDir = file("java-src/moongetter-client-okhttp")
project(":moongetter-client-cookie-java-net").projectDir = file("java-src/moongetter-client-cookie-java-net")
project(":moongetter-client-trustmanager-java-net").projectDir = file("java-src/moongetter-client-trustmanager-java-net")

project(":moongetter-android-robot").projectDir = file("android-src/moongetter-android-robot")

project(":moongetter-core").projectDir = file("kotlin-src/moongetter-core")
project(":moongetter-client").projectDir = file("kotlin-src/moongetter-client")
project(":moongetter-jsunpacker").projectDir = file("kotlin-src/moongetter-jsunpacker")
project(":moongetter-client-ktor").projectDir = file("kotlin-src/moongetter-client-ktor")
project(":moongetter-client-trustmanager").projectDir = file("kotlin-src/moongetter-client-trustmanager")
project(":moongetter-core-robot").projectDir = file("kotlin-src/moongetter-core-robot")
project(":moongetter-client-cookie-managment").projectDir = file("kotlin-src/moongetter-client-cookie-managment")


include(":moongetter-server-bundle")
include(":moongetter-server-robot-bundle")
