import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val moonGetterVersion: String by project
val javaStringVersion: String by project
val javaVirtualMachineTarget = JvmTarget.fromTarget(javaStringVersion)

plugins {
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
            artifactId = "moongetter-mediafire"
            version = moonGetterVersion
        }
    }
}

dependencies {
    implementation(project(":MoonGetter-Core"))
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.mock.web.server)
}