# Dependency and sample validation

Validated during the October 2026 sample migration. Versions are recorded in `gradle/libs.versions.toml`; the wrapper pins Gradle 9.7.0 with its distribution SHA-256.

| Component | Selected version |
| --- | --- |
| Kotlin and Compose compiler | 2.4.21 |
| Gradle / Android Gradle Plugin | 9.7.0 / 9.3.1 |
| Compose Multiplatform / Material3 | 1.12.1 / 1.9.0 |
| Ktor / OkHttp | 3.6.0 / 5.5.0 |
| Coroutines / Serialization | 1.11.0 / 1.11.0 |
| Koin | 4.2.2 |
| Chaintech media player / Media3 | 1.0.53 / 1.11.1 |
| JOGL and GlueGen | 2.6.0 |
| Cryptography / Okio / Datetime | 0.6.0 / 3.18.2 / 0.8.0 |
| Rhino / Maven Publish | 1.9.1 / 0.37.0 |
| Java toolchain | 21 |

Gradle and AGP are pinned to [Kotlin's supported range](https://kotlinlang.org/docs/gradle-configure-project.html), rather than their newest independent versions. JOGL and GlueGen must remain aligned. Compose Material3 and material icons have their own release versions. The modern [Android KMP library plugin](https://developer.android.com/kotlin/multiplatform/plugin) is used for shared sample code; the Android application uses AGP's built-in Kotlin support.

## Completed checks

- Android application and instrumentation APK assembly passed.
- Desktop classes and distributable packaging passed; the packaged UI opened.
- Shared iPhone and Apple Silicon simulator compilation passed.
- Signed physical-iPhone Xcode build passed with the relocated project (current scheme: `iosApp`).
- Automated provider suites passed: 84 JVM and 85 iOS simulator tests before the loading fix; the final core regression run included 85 JVM tests and 85 iOS simulator tests, with no failures in those suites.
- An explicitly configured VOE live test extracted the source and fetched its HLS master playlist successfully.
- The Android sample installed and launched in an emulator without a startup exception.

These checks do not establish playback for every provider on every platform. A follow-up packaged desktop test resolved Fireload in 1.889 s and left the extraction shimmer. The remaining player spinner was traced to the native VLC dependency: the installed Chaintech player reports VlcNotFound when discovery fails, and the sample previously ignored its error callback. The sample now displays an actionable playback error. VLC is not installed on this validation machine; desktop decode remains unverified. Fireload has a separately documented Darwin TLS failure. GoFile and 1fichier remain subject to live guest restrictions. Optional live tests that return early without configuration are excluded from live-success claims.

## Loading-state correction

The sample previously returned from success handlers when a provider returned no videos, leaving SEARCHING unchanged. A missing URL and exceptions outside the narrowly handled types could also leave it loading. It now applies a deadline to the complete extraction, cancels superseded work, transitions failures out of SEARCHING before emitting a notification, and uses final cleanup for unfinished requests. Cancellation is propagated.

The HTTP client is reused. There is no evidence yet that a minute-long cold-domain delay comes from library initialization. Distinguish extraction timing from native player startup, and measure DNS/TLS and each provider request before introducing caches or retries. Signed media URLs can expire, so caching them indiscriminately can create additional failures.

## Follow-up checks on October 10, 2026

The loading and cancellation fixes passed JVM compilation, Android APK assembly, and shared iPhone/simulator compilation. A JVM regression test explicitly verifies that cancellation escapes nullable resolution, fallback resolution, and multiple-URL resolution.

A fresh GET of the earlier VOE sample (`https://teresapoliticallearn.com/e/yo1xkhom61ev`) returned HTTP 404: DNS 0.121 s, TCP connection 0.193 s, TLS 0.352 s, first byte 0.569 s, and total 0.859 s. This does not reproduce the reported minute-long delay, and the earlier successful live test must not be interpreted as current availability of that resource. Reproducing the remaining delay requires the currently affected URL and platform. Extraction duration is logged separately from player startup without logging signed media URLs.

## Sample session and cold/warm validation

The JVM/Android cookie adapter used `CookiePolicy.ACCEPT_NONE` and serialized Ktor cookies using `Cookie.toString()`, which is a debug representation rather than a Set-Cookie header. It now accepts original-server cookies and uses [Ktor's header renderer](https://api.ktor.io/ktor-http/io.ktor.http/index.html). Regression tests verify round-trip values, secure cookies, path/domain scope, and rejection of unrelated domains. iOS already uses Ktor's matching cookie storage.

Before the correction, the sample client failed to resolve the Fireload ticket to its media host. After correction, the default Fireload URL produced one playback request on both attempts: first extraction 2.300 s, repeated extraction 1.572 s, client configuration 42.3 ms. Page and ticket requests completed successfully, with the ticket redirecting to `srv1.fireload.com`. No media decode or playback was performed by this timing test. These measurements rule out a minute-long initialization delay for this specific run; they do not rule out a provider/network delay on another URL or device.

To reproduce with a current link:

```shell
MOONGETTER_SAMPLE_TIMING_URL='https://www.fireload.com/5cab2fd19d300d84/So_Big!_(1932)_(HDrip_480p).mp4' ./gradlew :sample:shared:jvmTest --tests com.ead.project.moongetter.ColdWarmExtractionTest
```

The original run used Ktor/CIO and the Java cookie adapter. The test now uses the desktop sample’s OkHttp engine and performs two sequential extractions. Its XML report records client initialization, per-request status/final host/duration, and total extraction duration. No paths, signed media URLs, cookies or query tokens are logged. The normal sample logs these HTTP timings too, so a long shimmer can be tied to a particular request or to time between requests.

## Different-provider resource-access check

A fresh JVM test process also tested `https://my.mail.ru/video/embed/7512958666216054522#aylaz9ymde#7930`. The first source resolution took 3.510 s and returned two videos; repeated resolution took 3.107 s. Requesting the first byte of the selected source with its extracted headers returned HTTP 206 and `video/mp4`: first access 1.598 s, repeat 0.204 s. Each connection was closed after reading one byte; no full video download or decode was performed.

Enable the optional resource-access check with:

```shell
MOONGETTER_SAMPLE_TIMING_URL='https://my.mail.ru/video/embed/7512958666216054522#aylaz9ymde#7930' MOONGETTER_SAMPLE_VALIDATE_MEDIA=1 ./gradlew :sample:shared:jvmTest --tests com.ead.project.moongetter.ColdWarmExtractionTest
```

The source-resolution and media-access timings are separate. The latter currently uses the JVM HTTP connection stack and validates returned headers/source availability; it does not benchmark Android/iOS player buffering. This run did not reproduce the minute-long cold-load pattern.

The final packaged desktop UI check resolved Fireload in 1.873 s, displayed its single extracted video, and replaced the native player spinner with the explicit VLC prerequisite message. VLC installation was left to the user; source resolution is independently validated. Both cookie regression tests passed.

## Abyss integration

Abyss is now included in the bundle and used as the sample's initial URL. Its [provider validation](../../server-src/moongetter-abyss/README.md) records live extraction, MP4 decoding and seeking, offline JVM/iOS regressions, and local-proxy lifecycle requirements. The desktop sample and cold/warm test now use OkHttp instead of CIO because the new embed rejects CIO's TLS handshake with a protocol-version alert. Earlier timing measurements above retain the engine used at the time of those tests.

## Beta release checks on October 10, 2026

The release version is `2.0.0-beta01`. After the duplicate HEAD response
headers were corrected in Abyss and MEGA, JVM and native Darwin loopback
regression tests passed. The signed `iosApp` iPhone build passed and the
updated sample was installed. The user then confirmed that the iPhone
sample works. This confirms the tested Abyss sample, rather than all providers.

The separate live Darwin simulator probe failed with certificate-trust error
-1202 before retrieving the Abyss embed; the supplied physical-device log
retrieved that same embed with HTTP 200. TLS validation remains enabled.

The final beta check generated and inspected all 203 Maven publication POMs:
publication and internal dependency versions are `2.0.0-beta01`, with no sample
application publications. POM license declarations now match the MIT license
in the repository. JVM/iOS regression suites, Android application and test
APK assembly, desktop classes, and shared iPhone/simulator compilation passed.
This validates coordinates and build output, not Central upload/signing.

To regenerate the release POMs without uploading artifacts:

```shell
./gradlew generatePomFileForKotlinMultiplatformPublication generatePomFileForJvmPublication generatePomFileForIosArm64Publication generatePomFileForIosSimulatorArm64Publication generatePomFileForIosX64Publication generatePomFileForReleasePublication
```
