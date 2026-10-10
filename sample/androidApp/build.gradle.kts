import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val javaStringVersion: String = providers.gradleProperty("javaStringVersion").get()
val compileLibSdkVersion: String = providers.gradleProperty("compileLibSdkVersion").get()
val libSdkMinVersion: String = providers.gradleProperty("libSdkMinVersion").get()

kotlin {
    jvmToolchain(javaStringVersion.toInt())
    compilerOptions { jvmTarget.set(JvmTarget.fromTarget(javaStringVersion)) }
}
android {
    namespace = "com.ead.project.moongetter"
    compileSdk = compileLibSdkVersion.toInt()
    defaultConfig {
        applicationId = "com.ead.project.moongetter"
        minSdk = libSdkMinVersion.toInt()
        targetSdk = compileLibSdkVersion.toInt()
        versionCode = 3
        versionName = "1.03"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(javaStringVersion)
        targetCompatibility = JavaVersion.toVersion(javaStringVersion)
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    implementation(project(":sample:shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.media3.exoplayer)
    androidTestImplementation(libs.androidx.media3.exoplayer.hls)
    androidTestImplementation(project(":moongetter-core"))
    androidTestImplementation(project(":moongetter-mega"))
    androidTestImplementation(project(":moongetter-rpmstream"))
    androidTestImplementation(project(":moongetter-client-ktor"))
    androidTestImplementation(libs.ktor.client.okhttp)
    androidTestImplementation(libs.kotlinx.coroutines.core)
}
