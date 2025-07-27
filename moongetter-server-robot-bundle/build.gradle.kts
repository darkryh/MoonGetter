val moonGetterVersion: String by project

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    `maven-publish`
    signing
    id("com.vanniktech.maven.publish") version "0.34.0"
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = "com.ead.lib",
        artifactId = "moongetter-server-robot-bundle",
        version = moonGetterVersion
    )

    pom {
        name.set("MoonGetter Server Robot Bundle")
        description.set("Server Robot Bundle module for MoonGetter containing servers that are used with web tools")
        url.set("https://github.com/darkryh/MoonGetter")

        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
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
    val xcfName = "moongetter-server-robot-bundleKit"

    iosX64 {
        binaries.framework {
            baseName = xcfName
        }
    }

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
                val modules = arrayOf(":moongetter-fireload",":moongetter-onecloudfile")

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
    }
}