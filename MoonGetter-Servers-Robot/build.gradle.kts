import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val moonGetterVersion: String by project
val javaStringVersion: String by project
val javaVersion = JavaVersion.toVersion(javaStringVersion)
val javaVirtualMachineTarget = JvmTarget.fromTarget(javaStringVersion)

plugins {
    id("java-library")
    id("maven-publish")
    alias(libs.plugins.kotlin.jvm)
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
            artifactId = "moongetter-servers-robot"
            version = moonGetterVersion
        }
    }
}

dependencies {
    testImplementation(libs.junit)

    val modules = arrayOf(
        ":MoonGetter-Fireload", ":MoonGetter-Gofile", ":MoonGetter-OneCloudFile",
        ":MoonGetter-Vidguard"
    )

    modules.forEach { module ->
        api(project(module))
    }
}