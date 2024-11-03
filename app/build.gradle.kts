val javaStringVersion: String by project
val javaVersion = JavaVersion.toVersion(javaStringVersion)
val compileLibSdkVersion : String by project

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
        minSdk = 21
        targetSdk = compileLibSdkVersion.toInt()
        versionCode = 1
        versionName = "1.0"

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
    kotlinOptions {
        jvmTarget = javaStringVersion
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

    implementation(project(":Media3Player"))

    implementation(project(":MoonGetter-Core"))

    implementation(project(":MoonGetter-Abyss"))
    implementation(project(":MoonGetter-Doodstream"))
    implementation(project(":MoonGetter-Facebook"))
    implementation(project(":MoonGetter-Filemoon"))
    implementation(project(":MoonGetter-Fireload"))
    implementation(project(":MoonGetter-Gofile"))
    implementation(project(":MoonGetter-Goodstream"))
    implementation(project(":MoonGetter-GoogleDrive"))
    implementation(project(":MoonGetter-Hexload"))
    implementation(project(":MoonGetter-Lulustream"))
    implementation(project(":MoonGetter-Mediafire"))
    implementation(project(":MoonGetter-Mixdrop"))
    implementation(project(":MoonGetter-Mp4Upload"))
    implementation(project(":MoonGetter-Okru"))
    implementation(project(":MoonGetter-OneCloudFile"))
    implementation(project(":MoonGetter-Pixeldrain"))
    implementation(project(":MoonGetter-Senvid"))
    implementation(project(":MoonGetter-Streamtape"))
    implementation(project(":MoonGetter-Streamwish"))
    implementation(project(":MoonGetter-Uqload"))
    implementation(project(":MoonGetter-Vidguard"))
    implementation(project(":MoonGetter-Vihide"))
    implementation(project(":MoonGetter-Voe"))
    implementation(project(":MoonGetter-XTwitter"))
    implementation(project(":MoonGetter-YourUpload"))
}