# MoonGetter samples

`shared` contains the Compose UI and platform implementations. `androidApp`, `desktopApp`, and `iosApp` are the application entry points. Run Gradle commands from the repository root.

## Android

Install Android SDK 37, connect a device or start an emulator running API 26 or newer, and run:

```shell
./gradlew :sample:androidApp:installDebug
```

Use the **Android sample** run configuration after Gradle sync. The application ID is `com.ead.project.moongetter`.

## Desktop

```shell
./gradlew :sample:desktopApp:run
./gradlew :sample:desktopApp:createDistributable
```

The packaging task resolves Corretto Java 21 through the configured toolchain resolver. Network access is required on first use. Packaged applications are under `sample/desktopApp/build/compose/binaries/main/app`. Desktop playback requires native [VLC](https://www.videolan.org/vlc/) matching your operating system and architecture; the player does not bundle libvlc. Install VLC and restart the sample. A missing runtime now produces an explicit message. Compilation and opening the UI do not establish media decoding.

## iOS

```shell
open sample/iosApp/iosApp.xcodeproj
```

Select **iosApp**, choose an Apple Silicon simulator or physical iPhone, and build. For a physical device, select your own Apple development team in Signing & Capabilities. Xcode invokes `:sample:shared:embedAndSignAppleFrameworkForXcode` from the repository root. The framework remains named `ComposeApp`, matching the Swift import.

For Android Studio iOS run configurations, enable its Kotlin Multiplatform/iOS integration, reopen the project and sync Gradle. The registered Xcode project is `sample/iosApp/iosApp.xcodeproj`; The shared `iosApp` scheme is committed so new checkouts expose it. Xcode can build directly even if the IDE integration is unavailable.

## Change the sample

Edit [SampleConfiguration](shared/src/commonMain/kotlin/com/ead/project/moongetter/app/SampleConfiguration.kt) for the initial URL and the 60-second extraction deadline. Edit [AppModule](shared/src/commonMain/kotlin/com/ead/project/moongetter/app/AppModule.kt) for the factories. Platform HTTP engine, cookie and trust implementations are under each platform's `app/lib` package. Keep TLS certificate validation enabled.

If extraction fails, the UI must leave its loading state. The sample cancels a previous extraction when another starts, rejects empty playback results, and handles unexpected exceptions. A provider's request timeout is not evidence that its resource was taken down.

For first-load diagnostics, see [cold/warm validation](../docs/validation/dependencies.md). The sample logs each HTTP request's host, response status and duration, plus the complete extraction duration. Android/desktop session cookies are now retained with domain, path and secure matching. Compare the extraction completion timestamp with player startup to distinguish networking from buffering.

The initial URL for the upcoming release is now the [Abyss sample](https://argtesa.top/?v=vu9jynYrzV). Its playback URLs use a validated local range proxy, as documented in [the provider guide](../server-src/moongetter-abyss/README.md). Android/JVM use OkHttp for platform TLS; iOS uses Darwin. Playlist sessions stay open when switching qualities and are released when replacing the playlist or clearing the ViewModel.

The corrected Abyss proxy passed native Darwin HEAD/range regression tests, and the physical-iPhone sample was confirmed working on 2026-10-10. See [provider support](../README.md#providers) for current status and [beta migration notes](../CHANGELOG.md) before upgrading.
