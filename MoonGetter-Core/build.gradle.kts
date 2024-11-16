val moonGetterVersion: String by project
val javaStringVersion: String by project
val javaVersion = JavaVersion.toVersion(javaStringVersion)
val compileLibSdkVersion : String by project
val libSdkMinVersion : String by project

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin)
    id("maven-publish")
    kotlin("plugin.serialization") version "2.0.20"
}

android {
    namespace = "com.ead.lib.moongetter"
    compileSdk = compileLibSdkVersion.toInt()

    defaultConfig {
        minSdk = libSdkMinVersion.toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        jvmTarget = javaStringVersion
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(kotlin("reflect"))
    
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    api(libs.kotlinx.serialization.json)

    api("dev.datlag.jsunpacker:jsunpacker:1.0.2") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }
}