# MoonGetter

MoonGetter is a Kotlin Multiplatform library that resolves video-hosting links into playback requests: a URL, HTTP method, and any required headers. The checkout includes Android, desktop JVM, and iOS samples. Provider availability depends on the host, resource, network, and any access restrictions.

## Run the samples

The sample is separated into platform applications and shared Compose code:

```text
sample/
├── shared/       # UI, extraction configuration, platform services
├── androidApp/   # Android application
├── desktopApp/   # JVM application and packaging
└── iosApp/       # Xcode project and Swift application
```

From the repository root:

```shell
./gradlew :sample:androidApp:installDebug
./gradlew :sample:desktopApp:run
open sample/iosApp/iosApp.xcodeproj
```

Android installation requires a connected device or emulator. Desktop playback requires native [VLC](https://www.videolan.org/vlc/) matching your platform; the sample displays an error if it is unavailable. In Xcode, select the **iosApp** scheme and your device or an Apple Silicon simulator. Configure your own signing team for a physical device. Android Studio run configurations are in `.run/`; the iOS project location is registered in `.idea/xcode.xml`. See [sample setup](sample/README.md).

The old standalone Android application and Media3Player module have been removed. `sample/androidApp` now runs the shared Compose UI.

## Configure extraction

Change the initial URL and timeout in [SampleConfiguration](sample/shared/src/commonMain/kotlin/com/ead/project/moongetter/app/SampleConfiguration.kt). Configure provider factories in [AppModule](sample/shared/src/commonMain/kotlin/com/ead/project/moongetter/app/AppModule.kt). The sample uses a persistent client rather than constructing one for each URL.

This JVM example uses the same builder contract as the sample:

```kotlin
import com.ead.lib.moongetter.MoonFactory
import com.ead.lib.moongetter.MoonGetter
import com.ead.lib.moongetter.client.cookie.java.net.JavaNetCookieManagement
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.client.trust.manager.java.net.JavaMoonTrustManager
import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.builder.Engine
import com.ead.lib.moongetter.server.bundle.serverBundle
import io.ktor.client.engine.okhttp.OkHttp

fun initializeMoonGetter() {
    MoonGetter.start(
        MoonFactory.Builder()
            .setClient(KtorMoonClient(
                engineFactory = OkHttp,
                cookieManagement = JavaNetCookieManagement(),
                trustManager = JavaMoonTrustManager,
            ))
            .setTimeout(60_000L)
            .setEngine(Engine.Builder().onCore(serverBundle).build())
    )
}

suspend fun resolve(url: String): List<Request> =
    MoonGetter.get(url).videos.map { it.request }
```

Call initialization once before resolving links. Preserve the returned headers when handing a request to a player. Android and iOS use their respective platform implementations in `sample/shared`. `getOrNull` provides a nullable result; `getUntilFindResource` tries candidate links in order. Do not use a nullable result to diagnose a provider failure: use `get` and inspect the exception instead.

The next Maven release is **`2.0.0-beta01`**, advancing from `2.0.0-alpha02`. This beta contains breaking changes; see [the changelog](CHANGELOG.md) before upgrading. The version bump does not publish artifacts to Maven Central. The sample's Gradle project dependencies are the reproducible way to test the current implementation. Published artifacts use the group `io.github.darkryh.moongetter`; choose matching artifact versions when consuming a published release.

## Providers

The [server bundle](moongetter-server-bundle/src/commonMain/kotlin/com/ead/lib/moongetter/server/bundle/BundleFile.kt) registers **33 factories**: 29 enabled, two experimental, and two deprecated. Status below describes this checkout as of **2026-10-10**. An enabled implementation can still fail for a removed file, provider verification, expiring signatures, or network restrictions. Live checks cover the linked samples, rather than every file or platform.

**New** means added to the regular bundle since `2.0.0-alpha02`. **Updated** means the existing extractor changed. Fireload and 1CloudFile move from the removed Robot bundle into the regular bundle.

| Provider | Status | Beta change | Validation / limits |
| --- | --- | --- | --- |
| Abyss | Enabled | New | HTTP metadata and local MP4 proxy; JVM decode/seek passed, iPhone sample confirmed working after the HEAD fix; [guide](server-src/moongetter-abyss/README.md) |
| Doodstream | Enabled; live access blocked | Updated | Canonical redirect and request fixes; tested Dsvplay link failed TLS, fallback required verification; [details](server-src/moongetter-doodstream/VALIDATION.md) |
| Facebook | Enabled | Existing | Included; no fresh live validation in this beta |
| FileMoon / Byse | Enabled | Updated | HTTP verification and authenticated payload decryption; [validation](server-src/moongetter-filemoon/VALIDATION.md) |
| Fireload | Enabled | Migrated to HTTP | JVM source/decode passed; Darwin simulator certificate failure remains; [details](docs/validation/fireload.md) |
| GoFile | Experimental | New | Guest workflow implemented; live source retrieval blocked by rate limiting/access restrictions; [details](docs/validation/gofile.md) |
| GoodStream | Deprecated | Existing deprecation | Implementation and factory retained; excluded from active support |
| Google Drive | Enabled | Existing | Included; no fresh live validation in this beta |
| Hexload | Enabled | Existing | Included; no fresh live validation in this beta |
| LaMovie | Enabled | Existing | Included; no fresh live validation in this beta |
| Lulustream | Enabled | Existing | Included; no fresh live validation in this beta |
| Mail.ru | Enabled | New | MP4 sources and bounded media requests passed; [validation](server-src/moongetter-mailru/VALIDATION.md) |
| MediaFire | Enabled | Updated | Download-page/source parsing updated; portable regression coverage |
| MEGA | Enabled | New | Local decryption/range proxy; public files and selected/single-video folders; JVM decode/seek and Android device checks passed; [details](docs/validation/mega.md) |
| Mixdrop | Enabled | Existing | Included; no fresh live validation in this beta |
| Mp4Upload | Enabled | Existing | Included; no fresh live validation in this beta |
| OK.ru | Enabled | Existing | Included; no fresh live validation in this beta |
| 1CloudFile | Enabled | Migrated to HTTP | Source bytes and JVM decode/seek passed; [validation](docs/validation/onecloudfile.md) |
| 1fichier | Experimental | New | Free-download form flow; guest slots prevented a live source/seek check; [details](server-src/moongetter-onefichier/README.md) |
| Pixeldrain | Enabled | Existing | Included; no fresh live validation in this beta |
| RPMStream | Enabled | New | Native HLS extraction, JVM decode and Android playback/seek passed; [validation](docs/validation/rpmstream.md) |
| Savefiles | Enabled | New | Form-based HTTP extraction and HLS/media access passed; [guide](server-src/moongetter-savefiles/README.md) |
| Sendvid | Enabled | Existing | Included; no fresh live validation in this beta |
| Streamtape | Enabled | Existing | Included; no fresh live validation in this beta |
| Streamwish | Enabled | Existing | Included; no fresh live validation in this beta |
| Uqload | Enabled | Updated | Redirect identity, source and HLS checks updated; JVM media/decode passed; [validation](docs/validation/uqload.md) |
| Vidguard | Deprecated | Newly deprecated | Implementation retained for a possible return; no working public sample established |
| Vihide | Enabled | Updated | Domain matching updated; no fresh live validation in this beta |
| Vimeos | Enabled | New | Packed player/HLS extraction and fresh media requests passed; [guide](server-src/moongetter-vimeos/README.md) |
| VK | Enabled | Existing | Included; no fresh live validation in this beta |
| VOE | Enabled | Updated | Portable metadata/source decoding; earlier HLS check passed, supplied sample later returned 404; [validation](docs/validation/dependencies.md) |
| X/Twitter | Enabled | Existing | Included; no fresh live validation in this beta |
| YourUpload | Enabled | Existing | Included; no fresh live validation in this beta |

VOE remains enabled. The Robot/WebView extraction modules and their API are **removed**, with no compatibility layer. A verification response is an extraction/access failure, rather than proof that a video was taken down.

MEGA and Abyss return device-local playback URLs backed by decryption proxies. Keep their sessions alive while the playlist is in use, then call `MegaProxy.release(url)` or `AbyssProxy.release(url)` for each local URL. Call the corresponding `closeAll()` when shutting down the runtime. The sample manages this lifecycle; see the [MEGA](server-src/moongetter-mega/README.md) and [Abyss](server-src/moongetter-abyss/README.md) guides for direct integrations.

## Toolchain and dependencies

The project uses Kotlin **2.4.21**, Gradle **9.7.0**, AGP **9.3.1**, Compose Multiplatform **1.12.1**, Ktor **3.6.0**, and Java **21** toolchains. The selected Gradle/AGP pair stays inside [Kotlin's supported compatibility range](https://kotlinlang.org/docs/gradle-configure-project.html); it is intentionally below the newest standalone releases. Dependency versions are centralized in [the catalog](gradle/libs.versions.toml).

Android requires API **26** and compiles against API **37**. The minimum was raised for the upgraded Rhino runtime's JVM method-handle requirements. The Compose sample supports `iosArm64` and `iosSimulatorArm64`; Intel simulator support is not available for the latest sample dependencies. Desktop packaging uses an Amazon Corretto Java 21 toolchain, resolved through Foojay, and aligned JOGL/GlueGen versions.

## Validate changes

```shell
./gradlew :sample:androidApp:assembleDebug :sample:androidApp:assembleDebugAndroidTest
./gradlew :sample:desktopApp:classes :sample:desktopApp:createDistributable
./gradlew :sample:shared:compileKotlinIosArm64 :sample:shared:compileKotlinIosSimulatorArm64
./gradlew jvmTest iosSimulatorArm64Test test
```

Live tests are opt-in and require their provider-specific environment variables. A test that skips without its variable does not prove live extraction. Keep live requests infrequent to avoid triggering provider rate limits. See [upgrade validation](docs/validation/dependencies.md) for the checks performed and remaining limitations.

The Android/desktop cookie adapter now retains valid session cookies and renders proper HTTP cookie headers. A cold/warm Fireload extraction test returned a source in 2.30 seconds and 1.57 seconds respectively. The sample logs per-request and total extraction timing.

The sample limits the complete extraction operation to 60 seconds and clears its loading state on failure. That deadline is separate from individual HTTP request timeouts. DNS, TLS, redirects, provider waits, and player buffering can still add latency; a slow first request alone does not identify which stage is responsible.

## License

[MIT](LICENSE).
