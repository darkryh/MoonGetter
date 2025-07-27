import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val moonGetterVersion: String by project
val javaStringVersion: String by project
val javaVirtualMachineTarget = JvmTarget.fromTarget(javaStringVersion)


plugins {
    id("java-library")
    id("maven-publish")
    alias(libs.plugins.kotlin.jvm)
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
            artifactId = "moongetter-client-cookie-java-net"
            version = moonGetterVersion
        }
    }
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(project(":moongetter-client-cookie-managment"))

    testImplementation(libs.junit)
}