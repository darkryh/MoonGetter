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
    ":MoonGetter-Core",
    ":MoonGetter-Core-Robot"
)

include(
    ":MoonGetter-Servers",
    ":MoonGetter-Servers-Robot"
)

include(
    ":MoonGetter-Client",
    ":MoonGetter-Client-OkHttp",
    ":MoonGetter-Client-Ktor"
)

include(
    ":MoonGetter-Client-Cookie-Managment",
    ":MoonGetter-Client-Cookie-Java-NET"
)

include(
    ":MoonGetter-JsUnpacker"
)

include(
    ":MoonGetter-Client-TrustManager",
    ":MoonGetter-Client-TrustManager-Java-NET"
)

include(
    ":MoonGetter-Abyss",
    ":MoonGetter-Doodstream",
    ":MoonGetter-Facebook",
    ":MoonGetter-Filemoon",
    ":MoonGetter-Fireload",
    ":MoonGetter-Gofile",
    ":MoonGetter-Goodstream",
    ":MoonGetter-GoogleDrive",
    ":MoonGetter-Hexload",
    ":MoonGetter-Lulustream",
    ":MoonGetter-Mediafire",
    ":MoonGetter-Mixdrop",
    ":MoonGetter-Mp4Upload",
    ":MoonGetter-Okru",
    ":MoonGetter-OneCloudFile",
    ":MoonGetter-Pixeldrain",
    ":MoonGetter-Senvid",
    ":MoonGetter-Streamtape",
    ":MoonGetter-Streamwish",
    ":MoonGetter-Uqload",
    ":MoonGetter-VK",
    ":MoonGetter-Vidguard",
    ":MoonGetter-Vihide",
    ":MoonGetter-Voe",
    ":MoonGetter-XTwitter",
    ":MoonGetter-YourUpload"
)

include(":MoonGetter-Android-Robot")

include(":MoonGetter-JsUnpacker-Multiplatform")


project(":MoonGetter-Core").projectDir = file("kotlin-src/MoonGetter-Core")
project(":MoonGetter-Core-Robot").projectDir = file("kotlin-src/MoonGetter-Core-Robot")

project(":MoonGetter-Servers").projectDir = file("kotlin-src/MoonGetter-Servers")
project(":MoonGetter-Servers-Robot").projectDir = file("kotlin-src/MoonGetter-Servers-Robot")

project(":MoonGetter-Client").projectDir = file("kotlin-src/MoonGetter-Client")
project(":MoonGetter-Client-Ktor").projectDir = file("kotlin-src/MoonGetter-Client-Ktor")
project(":MoonGetter-Client-Cookie-Managment").projectDir = file("kotlin-src/MoonGetter-Client-Cookie-Managment")
project(":MoonGetter-JsUnpacker").projectDir = file("kotlin-src/MoonGetter-JsUnpacker")
project(":MoonGetter-Client-TrustManager").projectDir = file("kotlin-src/MoonGetter-Client-TrustManager")

project(":MoonGetter-Abyss").projectDir = file("kotlin-src/servers/MoonGetter-Abyss")
project(":MoonGetter-Doodstream").projectDir = file("kotlin-src/servers/MoonGetter-Doodstream")
project(":MoonGetter-Facebook").projectDir = file("kotlin-src/servers/MoonGetter-Facebook")
project(":MoonGetter-Filemoon").projectDir = file("kotlin-src/servers/MoonGetter-Filemoon")
project(":MoonGetter-Fireload").projectDir = file("kotlin-src/servers/MoonGetter-Fireload")
project(":MoonGetter-Gofile").projectDir = file("kotlin-src/servers/MoonGetter-Gofile")
project(":MoonGetter-Goodstream").projectDir = file("kotlin-src/servers/MoonGetter-Goodstream")
project(":MoonGetter-GoogleDrive").projectDir = file("kotlin-src/servers/MoonGetter-GoogleDrive")
project(":MoonGetter-Hexload").projectDir = file("kotlin-src/servers/MoonGetter-Hexload")
project(":MoonGetter-Lulustream").projectDir = file("kotlin-src/servers/MoonGetter-Lulustream")
project(":MoonGetter-Mediafire").projectDir = file("kotlin-src/servers/MoonGetter-Mediafire")
project(":MoonGetter-Mixdrop").projectDir = file("kotlin-src/servers/MoonGetter-Mixdrop")
project(":MoonGetter-Mp4Upload").projectDir = file("kotlin-src/servers/MoonGetter-Mp4Upload")
project(":MoonGetter-Okru").projectDir = file("kotlin-src/servers/MoonGetter-Okru")
project(":MoonGetter-OneCloudFile").projectDir = file("kotlin-src/servers/MoonGetter-OneCloudFile")
project(":MoonGetter-Pixeldrain").projectDir = file("kotlin-src/servers/MoonGetter-Pixeldrain")
project(":MoonGetter-Senvid").projectDir = file("kotlin-src/servers/MoonGetter-Senvid")
project(":MoonGetter-Streamtape").projectDir = file("kotlin-src/servers/MoonGetter-Streamtape")
project(":MoonGetter-Streamwish").projectDir = file("kotlin-src/servers/MoonGetter-Streamwish")
project(":MoonGetter-Uqload").projectDir = file("kotlin-src/servers/MoonGetter-Uqload")
project(":MoonGetter-VK").projectDir = file("kotlin-src/servers/MoonGetter-VK")
project(":MoonGetter-Vidguard").projectDir = file("kotlin-src/servers/MoonGetter-Vidguard")
project(":MoonGetter-Vihide").projectDir = file("kotlin-src/servers/MoonGetter-Vihide")
project(":MoonGetter-Voe").projectDir = file("kotlin-src/servers/MoonGetter-Voe")
project(":MoonGetter-XTwitter").projectDir = file("kotlin-src/servers/MoonGetter-XTwitter")
project(":MoonGetter-YourUpload").projectDir = file("kotlin-src/servers/MoonGetter-YourUpload")


project(":MoonGetter-Client-OkHttp").projectDir = file("java-src/MoonGetter-Client-OkHttp")
project(":MoonGetter-Client-Cookie-Java-NET").projectDir = file("java-src/MoonGetter-Client-Cookie-Java-NET")
project(":MoonGetter-Client-TrustManager-Java-NET").projectDir = file("java-src/MoonGetter-Client-TrustManager-Java-NET")

project(":MoonGetter-Android-Robot").projectDir = file("android-src/MoonGetter-Android-Robot")

project(":MoonGetter-JsUnpacker-Multiplatform").projectDir = file("kotlin-multiplatform-src/MoonGetter-JsUnpacker-Multiplatform")