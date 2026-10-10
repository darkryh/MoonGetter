import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

val javaStringVersion: String = providers.gradleProperty("javaStringVersion").get()
val compileLibSdkVersion: String = providers.gradleProperty("compileLibSdkVersion").get()
val libSdkMinVersion: String = providers.gradleProperty("libSdkMinVersion").get()

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(javaStringVersion.toInt())
    android {
        namespace = "com.ead.project.moongetter.shared"
        compileSdk = compileLibSdkVersion.toInt()
        minSdk = libSdkMinVersion.toInt()
        androidResources.enable = true
        compilerOptions { jvmTarget.set(JvmTarget.fromTarget(javaStringVersion)) }
    }

    val xcf = XCFramework()


    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true

            binaryOption("bundleId", "com.ead.project.moongetter.iosApp")
            xcf.add(this)
        }
    }

    jvm()

    sourceSets {
        val jvmMain = getByName("jvmMain")

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)

            implementation (libs.androidx.media3.exoplayer)
            implementation (libs.androidx.media3.ui)
            implementation (libs.androidx.media3.exoplayer.hls)

            implementation(project(":moongetter-client-cookie-java-net"))
            implementation(project(":moongetter-client-trustmanager-java-net"))
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.compose.material.icons)

            implementation(project(":moongetter-core"))

            implementation(project(":moongetter-client-ktor"))
            implementation(project(":moongetter-server-bundle"))

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.media.player)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            implementation(libs.ktor.client.okhttp)
            implementation(project(":moongetter-client-cookie-java-net"))
            implementation(project(":moongetter-client-trustmanager-java-net"))

            implementation(libs.media.player)
            implementation(libs.gluegen)
            implementation(libs.jogl)
        }
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    enabled = false
}

tasks.withType<PublishToMavenLocal>().configureEach {
    enabled = false
}

compose.resources {
    publicResClass = true
    packageOfResClass = "moongetter.composeapp.generated.resources"
}
