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
    namespace = "com.ead.lib.moongetter.servers"
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
            artifactId = "moongetter-servers"
            version = moonGetterVersion
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    val modules = arrayOf(
        ":MoonGetter-Abyss", ":MoonGetter-Doodstream", ":MoonGetter-Facebook",
        ":MoonGetter-Filemoon",":MoonGetter-Goodstream", ":MoonGetter-GoogleDrive",
        ":MoonGetter-Hexload",":MoonGetter-Lulustream",":MoonGetter-Mediafire",
        ":MoonGetter-Mixdrop", ":MoonGetter-Mp4Upload",":MoonGetter-Okru",
        ":MoonGetter-Pixeldrain", ":MoonGetter-Senvid", ":MoonGetter-Streamtape",
        ":MoonGetter-Streamwish", ":MoonGetter-Uqload", ":MoonGetter-Vihide",
        ":MoonGetter-Voe", ":MoonGetter-XTwitter", ":MoonGetter-YourUpload"
    )

    modules.forEach { module ->
        api(project(module))
    }
}