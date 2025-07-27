import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val javaStringVersion: String by project
val javaVersion = JavaVersion.toVersion(javaStringVersion)
val compileLibSdkVersion : String by project
val libSdkMinVersion : String by project

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.ead.project.moongetter"
    compileSdk = compileLibSdkVersion.toInt()

    defaultConfig {
        applicationId = "com.ead.project.moongetter"
        minSdk = libSdkMinVersion.toInt()
        targetSdk = compileLibSdkVersion.toInt()
        versionCode = 2
        versionName = "1.01"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaStringVersion))
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.koin.android)
    testImplementation(libs.koin.test)

    implementation(libs.ktor.client.cio)

    implementation(project(":Media3Player"))
    implementation(project(":moongetter-core"))
    implementation(project(":moongetter-core-robot"))
    implementation(project(":moongetter-client-okhttp"))

    implementation(project(":moongetter-client-cookie-java-net"))
    implementation(project(":moongetter-client-trustmanager-java-net"))

    implementation(project(":moongetter-android-robot"))

    implementation(project(":moongetter-client-ktor"))
    implementation(project(":moongetter-server-bundle"))
    implementation(project(":moongetter-server-robot-bundle"))
}