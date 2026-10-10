# 1CloudFile validation

Sample: `https://1cloudfile.com/Qhji`, checked on 2026-10-09. The page lists a 13.47 MB MP4 file. Its decoded signed URL is on `fs21.1cloudfile.com`.

## HTTP workflow

1. Request the public file page through MoonClient.
2. Verify that its final URL remains the same file on 1CloudFile.
3. Extract the hex string passed to the download-link function and decode the current XOR encoding in common Kotlin. The page also uses WebAssembly, but it is not necessary for decoding this link or requesting this file.
4. Reject URLs outside the provider's HTTPS file servers or unsupported video extensions.
5. Validate the signed source with a bodyless request. KtorMoonClient maps GET with `isResponseBodyNeeded = false` to HEAD. Require a successful response and a supported video/binary content type, rejecting unexpected media redirects.
6. Return the signed media URL with the canonical Referer and site Origin. Pass those headers to the player.

This migration moves `moongetter-onecloudfile` from `server-robot-src` to `server-src`, removes its Robot API dependency, and registers it in the regular server bundle. The current sample apps start with the Fireload example; use this URL to test 1CloudFile.

## Checks

A direct range request returned HTTP 206, `video/mp4`, `Content-Range: bytes 0-1023/14127626`, and an MP4 `ftyp` header. It downloaded only 1024 bytes for that probe.

```sh
./gradlew :moongetter-onecloudfile:jvmTest :moongetter-onecloudfile:iosSimulatorArm64Test :sample:shared:compileKotlinIosArm64
MOONGETTER_ONECLOUD_TEST_URL=https://1cloudfile.com/Qhji MOONGETTER_ONECLOUD_FFMPEG=1 ./gradlew :moongetter-onecloudfile:jvmTest --rerun-tasks
```

The optional live test runs the real extractor and decodes three seconds from the start and after seeking to ten seconds. Both checks passed, along with all four common tests on JVM and the iOS simulator. The signed iPhone sample build also passed. Deterministic common tests cover decoding, signed query preservation, unsupported inputs/hosts, caller headers, bodyless ticket checking, HTML placeholders, and unrelated redirects.

The fixed decoder is specific to the site's current public download encoding. If it changes, extraction reports an unsupported-encoding error; there is no silent WebView fallback. Signed ticket expiry or per-IP restrictions can require fresh extraction. Native parser tests and device compilation do not establish actual AVPlayer playback; verify playback and seeking on the physical iPhone separately.
