import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val moonGetterVersion: String = providers.gradleProperty("moonGetterVersion").get()
val javaStringVersion: String = providers.gradleProperty("javaStringVersion").get()
val javaVirtualMachineTarget = JvmTarget.fromTarget(javaStringVersion)

plugins {
    id("maven-publish")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.toVersion(javaStringVersion)
    targetCompatibility = JavaVersion.toVersion(javaStringVersion)
}

kotlin {
    compilerOptions {
        jvmTarget = javaVirtualMachineTarget
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            groupId = "io.github.darkryh.moongetter"
            artifactId = "moongetter-client-trustmanager-java-net"
            version = moonGetterVersion
        }
    }
}

dependencies {
    api(project(":moongetter-client-trustmanager"))
}