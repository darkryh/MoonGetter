import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val moonGetterVersion: String by project
val javaStringVersion: String by project
val javaVersion = JavaVersion.toVersion(javaStringVersion)
val compileLibSdkVersion : String by project
val libSdkMinVersion : String by project

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin)
    id("maven-publish")
}

android {
    namespace = "com.ead.lib.moongetter.android.robot"
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
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaStringVersion))
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
            groupId = "io.github.darkryh.moongetter"
            artifactId = "moongetter-android-robot"
            version = moonGetterVersion
        }
    }
}

dependencies {
    implementation(project(":moongetter-core"))
    implementation(libs.androidx.webkit)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.mock.web.server)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}