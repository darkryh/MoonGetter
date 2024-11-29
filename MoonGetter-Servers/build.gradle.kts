import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val moonGetterVersion: String by project
val javaStringVersion: String by project
val javaVersion = JavaVersion.toVersion(javaStringVersion)
val javaVirtualMachineTarget = JvmTarget.fromTarget(javaStringVersion)

plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    id("maven-publish")
}

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
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
        ":MoonGetter-Hexload",":MoonGetter-Lulustream",":MoonGetter-Mediafire",
        ":MoonGetter-Mixdrop", ":MoonGetter-Mp4Upload",":MoonGetter-Okru",
        ":MoonGetter-Pixeldrain", ":MoonGetter-Senvid", ":MoonGetter-Streamtape",
        ":MoonGetter-Streamwish", ":MoonGetter-Uqload", ":MoonGetter-Vihide",
        ":MoonGetter-Voe", ":MoonGetter-XTwitter", ":MoonGetter-YourUpload"
    )

    modules.forEach { module ->
        api(project(module))
    }
}