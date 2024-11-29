import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val moonGetterVersion: String by project
val javaStringVersion: String by project
val javaVersion = JavaVersion.toVersion(javaStringVersion)
val javaVirtualMachineTarget = JvmTarget.fromTarget(javaStringVersion)

plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    id("maven-publish")
    kotlin("plugin.serialization") version "2.0.21"
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
            artifactId = "moongetter-core"
            version = moonGetterVersion
        }
    }
}

dependencies {
    api(libs.okhttp)

    api(libs.moshi)
    api(libs.moshi.kotlin)
    api(libs.kotlinx.serialization.json)

    api(libs.kotlinx.coroutines.core)
    implementation(kotlin("reflect"))
    
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
    
    api("dev.datlag.jsunpacker:jsunpacker:1.0.2") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }
}