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
            artifactId = "moongetter-client-okhttp"
            version = moonGetterVersion
        }
    }
}

dependencies {
    implementation(project(":MoonGetter-Client"))
    implementation(libs.okhttp)
    implementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.mock.web.server)
}