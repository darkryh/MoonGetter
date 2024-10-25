pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MoonGetter"
include(":app")
include(":Media3Player")
include(":MoonGetter-Core")
include(":MoonGetter-Doodstream")
include(":MoonGetter-Facebook")
include(":MoonGetter-Filemoon")
include(":MoonGetter-Fireload")
include(":MoonGetter-Gofile")
include(":MoonGetter-Goodstream")
include(":MoonGetter-GoogleDrive")
include(":MoonGetter-Hexload")
include(":MoonGetter-Lulustream")
include(":MoonGetter-Mediafire")
include(":MoonGetter-Mixdrop")
include(":MoonGetter-Mp4Upload")
include(":MoonGetter-Okru")
include(":MoonGetter-OneCloudFile")
include(":MoonGetter-Pixeldrain")
include(":MoonGetter-Senvid")
include(":MoonGetter-Streamtape")
include(":MoonGetter-Streamwish")
include(":MoonGetter-Uqload")
include(":MoonGetter-Vidguard")
include(":MoonGetter-Vihide")
include(":MoonGetter-Voe")
include(":MoonGetter-XTwitter")
include(":MoonGetter-YourUpload")