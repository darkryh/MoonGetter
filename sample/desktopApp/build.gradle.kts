import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
}
val javaStringVersion: String = providers.gradleProperty("javaStringVersion").get()
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(javaStringVersion.toInt()))
        vendor.set(JvmVendorSpec.AMAZON)
    }
}

dependencies {
    implementation(project(":sample:shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.resources)
    implementation(libs.media.player)
}
compose.desktop {
    application {
        javaHome = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(javaStringVersion.toInt()))
            vendor.set(JvmVendorSpec.AMAZON)
        }.get().metadata.installationPath.asFile.absolutePath
        mainClass = "com.ead.project.moongetter.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.ead.project.moongetter"
            packageVersion = "1.0.0"
            macOS { iconFile.set(project.file("../shared/src/commonMain/composeResources/drawable/ic_app_desktop.icns")) }
            windows { iconFile.set(project.file("../shared/src/commonMain/composeResources/drawable/ic_app_desktop.ico")) }
            linux { iconFile.set(project.file("../shared/src/commonMain/composeResources/drawable/ic_app_desktop.png")) }
        }
    }
}
