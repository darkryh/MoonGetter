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
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://jogamp.org/deployment/maven/") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}


rootProject.name = "MoonGetter"
include(":composeApp")
include(":androidApp")
include(":Media3Player")

// bundles
include(":moongetter-server-bundle")
include(":moongetter-server-robot-bundle")


// java sources
include(":moongetter-client-okhttp")
project(":moongetter-client-okhttp").projectDir = file("java-src/moongetter-client-okhttp")

include(
    ":moongetter-client-cookie-java-net",
    ":moongetter-client-trustmanager-java-net"
)
project(":moongetter-client-cookie-java-net").projectDir = file("java-src/moongetter-client-cookie-java-net")
project(":moongetter-client-trustmanager-java-net").projectDir = file("java-src/moongetter-client-trustmanager-java-net")

// android sources
include(":moongetter-android-robot")
project(":moongetter-android-robot").projectDir = file("android-src/moongetter-android-robot")


// moon sources
include(
    ":moongetter-core",
    ":moongetter-core-robot",
    ":moongetter-client",
    ":moongetter-jsunpacker",
    ":moongetter-client-ktor",
    ":moongetter-client-trustmanager",
    ":moongetter-client-cookie-managment"
)

project(":moongetter-core").projectDir = file("kotlin-src/moongetter-core")
project(":moongetter-core-robot").projectDir = file("kotlin-src/moongetter-core-robot")
project(":moongetter-client").projectDir = file("kotlin-src/moongetter-client")
project(":moongetter-jsunpacker").projectDir = file("kotlin-src/moongetter-jsunpacker")
project(":moongetter-client-ktor").projectDir = file("kotlin-src/moongetter-client-ktor")
project(":moongetter-client-trustmanager").projectDir = file("kotlin-src/moongetter-client-trustmanager")
project(":moongetter-client-cookie-managment").projectDir = file("kotlin-src/moongetter-client-cookie-managment")


// server impl
include(":moongetter-doodstream")
include(":moongetter-facebook")
include(":moongetter-filemoon")
include(":moongetter-goodstream")
include(":moongetter-googledrive")
include(":moongetter-hexload")
include(":moongetter-lamovie")
include(":moongetter-lulustream")
include(":moongetter-mediafire")
include(":moongetter-mixdrop")
include(":moongetter-mp4upload")
include(":moongetter-okru")
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

project(":moongetter-doodstream").projectDir = file("server-src/moongetter-doodstream")
project(":moongetter-facebook").projectDir = file("server-src/moongetter-facebook")
project(":moongetter-filemoon").projectDir = file("server-src/moongetter-filemoon")
project(":moongetter-goodstream").projectDir = file("server-src/moongetter-goodstream")
project(":moongetter-googledrive").projectDir = file("server-src/moongetter-googledrive")
project(":moongetter-hexload").projectDir = file("server-src/moongetter-hexload")
project(":moongetter-lamovie").projectDir = file("server-src/moongetter-lamovie")
project(":moongetter-lulustream").projectDir = file("server-src/moongetter-lulustream")
project(":moongetter-mediafire").projectDir = file("server-src/moongetter-mediafire")
project(":moongetter-mixdrop").projectDir = file("server-src/moongetter-mixdrop")
project(":moongetter-mp4upload").projectDir = file("server-src/moongetter-mp4upload")
project(":moongetter-okru").projectDir = file("server-src/moongetter-okru")
project(":moongetter-pixeldrain").projectDir = file("server-src/moongetter-pixeldrain")
project(":moongetter-senvid").projectDir = file("server-src/moongetter-senvid")
project(":moongetter-streamtape").projectDir = file("server-src/moongetter-streamtape")
project(":moongetter-streamwish").projectDir = file("server-src/moongetter-streamwish")
project(":moongetter-uqload").projectDir = file("server-src/moongetter-uqload")
project(":moongetter-vidguard").projectDir = file("server-src/moongetter-vidguard")
project(":moongetter-vihide").projectDir = file("server-src/moongetter-vihide")
project(":moongetter-vk").projectDir = file("server-src/moongetter-vk")
project(":moongetter-voe").projectDir = file("server-src/moongetter-voe")
project(":moongetter-xtwitter").projectDir = file("server-src/moongetter-xtwitter")
project(":moongetter-yourupload").projectDir = file("server-src/moongetter-yourupload")


// server robot impl
include(":moongetter-fireload")
include(":moongetter-onecloudfile")

project(":moongetter-fireload").projectDir = file("server-robot-src/moongetter-fireload")
project(":moongetter-onecloudfile").projectDir = file("server-robot-src/moongetter-onecloudfile")
