# Changelog

## Unreleased

### Added

- Route the Abyss, Doodstream, Filemoon, Lulustream, Mixdrop, RPMStream,
  Streamtape, Streamwish, Vidhide and Voe servers by a `ServerDescriptor`:
  exact hostnames from a researched registry (`tools/domain-registry`), path
  shapes and HTML fingerprints. The registry lists about 230 more mirror
  domains than the previous patterns named explicitly.
- Add a network fallback, on by default (`ResolutionConfig(fallbackEnabled = false)`
  disables it), that identifies URLs on unlisted hosts through redirects, embedded frames,
  page fingerprints and, for servers sharing XFileSharing pages, extractor
  probing. Confirmed hosts are remembered and reported through a listener.
- Add `MoonGetter.resolve(url)` to inspect how a URL would be resolved, and
  `Error.AMBIGUOUS_URL_ENTRY` for URLs that several servers claim.
- Add `checkHostRegistry`/`generateHostRegistry` Gradle tasks.

### Changed

- URLs for the servers above are now matched on the whole hostname and
  anchored. Unlisted look-alike names that the previous loose patterns
  accepted (for example `do9streamxyz.io`) are no longer routed without the
  fallback.
- A URL claimed by two servers now fails with `AMBIGUOUS_URL_ENTRY` instead of
  `UNKNOWN_URL_ENTRY`. Exhaustive `when` expressions over `Error` need the new
  branch.
- Mixdrop sends the mirror's own origin as `Referer`/`Origin` instead of a
  fixed `mixdrop.co`.

### Fixed

- A client passed to a later `MoonGetter.start` replaces the previous client
  instead of being ignored.
- Each server instance receives its own copy of the configured headers, so one
  server's `Referer`/`Origin` changes no longer leak into later requests.

## 2.0.0-beta01 - 2026-10-10

This is the first beta of 2.0.0. The release is prepared for Maven publication;
changing the repository version does not publish it. Provider status and
validation limits are listed in the [README](README.md#providers).

### Added

- Add Abyss, Mail.ru, MEGA, RPMStream, Savefiles, and Vimeos to the regular
  server bundle. Add experimental guest extractors for GoFile and 1fichier.
- Stream MEGA through a device-local decryption proxy, including a selected
  file in a public folder or a folder containing exactly one video. Ambiguous
  folders require a file selector.
- Stream Abyss split MP4 files through a local range proxy. The player’s
  Google Storage placeholder is replaced with a validated local playback URL.

### Changed

- Move Fireload and 1CloudFile into the regular HTTP server bundle. They no
  longer require a WebView.
- Organize samples under `sample/`: shared Compose code, Android and desktop
  entry points, and the iOS Xcode application.
- Update the Kotlin, Gradle, Compose, Ktor, and supporting dependencies. The
  selected versions are in `gradle/libs.versions.toml`.
- Raise the Android sample minimum to API 26 and use Java 21 toolchains for
  JVM compilation. All iOS modules target physical ARM64 devices
  and Apple Silicon simulators. Intel simulator (`iosX64`) artifacts are removed.
- Use a 60-second default request timeout and a separate 60-second deadline
  for the sample’s complete extraction operation.

### Fixed

- Clear the sample’s loading state when extraction throws, returns no videos,
  or is replaced by another request. Propagate coroutine cancellation through
  nullable, fallback, and batch resolution APIs.
- Retain valid Android/JVM session cookies and serialize them as HTTP cookie
  headers. Log request duration separately from player startup.
- Update VOE decoding and regexes for Kotlin Multiplatform. Update Uqload
  redirect/source validation and MediaFire parsing.
- Resolve Doodstream canonical redirects with the same video identifier and
  preserve the final embed’s request headers. TLS and verification failures
  now report access errors; the tested source remains blocked.
- Add FileMoon/Byse HTTP verification and authenticated payload decoding.
- Avoid duplicate local-proxy HEAD headers rejected by iOS with error -1017.
  The updated Abyss sample was confirmed working on a physical iPhone.
- Report a missing native VLC runtime in the desktop sample instead of
  leaving its player loading indefinitely.

### Deprecated

- Mark Vidguard deprecated after no working public sample could be validated.
  GoodStream remains deprecated. Both factories and implementations remain.

### Removed

- Remove the Robot/WebView API and modules, including `Robot`, `ServerRobot`,
  `ServerJwPlayer`, `Engine.Builder.onRobot`, the Android Robot implementation,
  and the Robot server bundle. There is no compatibility layer.
- Remove the unused standalone Android app and Media3Player module.
- Stop tracking Kotlin compiler metadata, IDE caches, and Xcode per-user state.

### Upgrade from 2.0.0-alpha02

Use the same `2.0.0-beta01` version for every MoonGetter artifact once the beta
is available in Maven Central. Replace Robot bundle dependencies with
`moongetter-server-bundle`, remove Robot/WebView initialization, and configure
factories with `Engine.Builder().onCore(serverBundle).build()`. Update custom
server constructors that used the removed Robot parameter.

Update sample task paths from `:composeApp` and `:androidApp` to `:sample:shared`
and `:sample:androidApp`; launch desktop with `:sample:desktopApp:run`. Open
`sample/iosApp/iosApp.xcodeproj` and select the `iosApp` scheme. Configure your
own signing team and use an Apple Silicon simulator or physical iPhone. Replace
`iosX64` with `iosSimulatorArm64` in consumer modules that target simulators.

Preserve each returned playback request’s headers. MEGA and Abyss return
URLs on the current device’s loopback listener; retain those sessions while
playing and release every playlist URL when it is replaced or discarded.
See the [MEGA](server-src/moongetter-mega/README.md) and
[Abyss](server-src/moongetter-abyss/README.md) lifecycle guides.
