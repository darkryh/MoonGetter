import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val moonGetterVersion: String by project
val javaStringVersion: String by project
val javaVirtualMachineTarget = JvmTarget.fromTarget(javaStringVersion)

plugins {
    id("java-library")
    id("maven-publish")
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        jvmTarget = javaVirtualMachineTarget
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["java"])
            }

            groupId = "com.ead.lib"
            artifactId = "moongetter-servers"
            version = moonGetterVersion
        }
    }
}

dependencies {
    testImplementation(libs.junit)
    val modules = arrayOf(
        ":MoonGetter-Abyss", ":MoonGetter-Doodstream", ":MoonGetter-Facebook",
        ":MoonGetter-Filemoon",":MoonGetter-Goodstream", ":MoonGetter-GoogleDrive",
        ":MoonGetter-Hexload",":MoonGetter-Lulustream", ":MoonGetter-LaMovie",
        ":MoonGetter-Mediafire",
        ":MoonGetter-Mixdrop", ":MoonGetter-Mp4Upload",":MoonGetter-Okru",
        ":MoonGetter-Pixeldrain", ":MoonGetter-Senvid", ":MoonGetter-Streamtape",
        ":MoonGetter-Streamwish", ":MoonGetter-Uqload", ":MoonGetter-Vihide",
        ":MoonGetter-Voe", ":MoonGetter-XTwitter", ":MoonGetter-YourUpload",
        ":MoonGetter-Vidguard", ":MoonGetter-VK"
    )

    modules.forEach { module ->
        api(project(module))
    }
}