val moonGetterVersion: String = providers.gradleProperty("moonGetterVersion").get()

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
    signing
    alias(libs.plugins.maven.publish)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = "io.github.darkryh.moongetter",
        artifactId = "moongetter-server-bundle",
        version = moonGetterVersion
    )

    pom {
        name.set("MoonGetter Server Bundle")
        description.set("Server Bundle module for MoonGetter containing native servers")
        url.set("https://github.com/darkryh/MoonGetter")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
            }
        }

        scm {
            url.set("https://github.com/darkryh/MoonGetter")
            connection.set("scm:git:git://github.com/darkryh/MoonGetter.git")
            developerConnection.set("scm:git:ssh://github.com/darkryh/MoonGetter.git")
        }

        developers {
            developer {
                id.set("Darkryh")
                name.set("Xavier Alexander Torres Calderón")
                email.set("alex_torres-xc@hotmail.com")
            }
        }
    }
}

kotlin {
    jvm()
    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
    val xcfName = "moongetter-server-bundleKit"

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // Add KMP dependencies here

                val modules = arrayOf(
                    ":moongetter-abyss",
                    ":moongetter-fireload", ":moongetter-onecloudfile", ":moongetter-doodstream", ":moongetter-facebook",
                    ":moongetter-filemoon",":moongetter-goodstream", ":moongetter-googledrive",
                    ":moongetter-hexload",":moongetter-lulustream", ":moongetter-lamovie",
                    ":moongetter-mediafire", ":moongetter-mixdrop", ":moongetter-mp4upload",
                    ":moongetter-okru", ":moongetter-pixeldrain", ":moongetter-senvid",
                    ":moongetter-streamtape", ":moongetter-streamwish", ":moongetter-uqload",
                    ":moongetter-mega", ":moongetter-rpmstream", ":moongetter-mailru", ":moongetter-gofile", ":moongetter-savefiles", ":moongetter-onefichier", ":moongetter-vimeos", ":moongetter-vihide", ":moongetter-voe", ":moongetter-xtwitter",
                    ":moongetter-yourupload", ":moongetter-vidguard", ":moongetter-vk"
                )

                api(project(":moongetter-core"))

                modules.forEach { module ->
                    api(project(module))
                }
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}

// Cross-server tests read tools/domain-registry/registry.json from the repository root.
tasks.withType<Test>().configureEach {
    systemProperty("moongetter.root", rootDir.absolutePath)
    inputs.file(rootProject.file("tools/domain-registry/registry.json"))
    // The live domain drift check only runs when explicitly requested.
    providers.environmentVariable("MOONGETTER_DOMAIN_DRIFT").orNull?.let { environment("MOONGETTER_DOMAIN_DRIFT", it) }
}