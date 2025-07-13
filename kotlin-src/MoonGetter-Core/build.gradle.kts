import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val moonGetterVersion: String by project
val javaStringVersion: String by project
val javaVirtualMachineTarget = JvmTarget.fromTarget(javaStringVersion)

plugins {
    id("maven-publish")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
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
            artifactId = "moongetter-core"
            version = moonGetterVersion
        }
    }
}


dependencies {
    api(project(":MoonGetter-Client"))
    api(project(":MoonGetter-JsUnpacker"))

    api(libs.moshi)
    api(libs.moshi.kotlin)
    api(libs.kotlinx.serialization.json)
    api(libs.ktor.http)
    api(libs.kotlinx.coroutines.core)
    
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.mock.web.server)
}