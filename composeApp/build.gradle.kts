import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

val javaStringVersion: String by project
val javaVersion = JavaVersion.toVersion(javaStringVersion)
val compileLibSdkVersion : String by project
val libSdkMinVersion : String by project

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(javaStringVersion))
        }
    }

    val xcf = XCFramework()


    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            xcf.add(this)
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.ktor.client.okhttp)

            implementation (libs.androidx.media3.exoplayer)
            implementation (libs.androidx.media3.ui)
            implementation (libs.androidx.media3.exoplayer.hls)

            implementation(project(":moongetter-client-cookie-java-net"))
            implementation(project(":moongetter-client-trustmanager-java-net"))
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(compose.materialIconsExtended)

            implementation(project(":moongetter-core"))
            implementation(project(":moongetter-core-robot"))

            implementation(project(":moongetter-client-ktor"))
            implementation(project(":moongetter-server-bundle"))
            implementation(project(":moongetter-server-robot-bundle"))

            val koin_version = "4.0.3"
            implementation(project.dependencies.platform("io.insert-koin:koin-bom:$koin_version"))
            implementation("io.insert-koin:koin-core")
            implementation("io.insert-koin:koin-compose:${koin_version}")
            implementation("io.insert-koin:koin-compose-viewmodel:${koin_version}")
            implementation("io.insert-koin:koin-compose-viewmodel-navigation:${koin_version}")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            implementation(libs.ktor.client.cio)
            implementation(project(":moongetter-client-cookie-java-net"))
            implementation(project(":moongetter-client-trustmanager-java-net"))
        }
    }
}

android {
    namespace = "com.ead.project.moongetter"
    compileSdk = compileLibSdkVersion.toInt()

    defaultConfig {
        applicationId = "com.ead.project.moongetter"
        minSdk = libSdkMinVersion.toInt()
        targetSdk = compileLibSdkVersion.toInt()
        versionCode = 2
        versionName = "1.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.ead.project.moongetter.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.ead.project.moongetter"
            packageVersion = "1.0.0"
        }
    }
}