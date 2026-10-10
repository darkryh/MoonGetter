# RPMStream validation — October 9, 2026

Supplied public embed: [pelisplus.rpmstream.live/#b1lamw](https://pelisplus.rpmstream.live/#b1lamw).

The root HTML loads `assets/index-B82x0F06.js`. That script uses a fragment video ID and a public `/api/v1/video` request, then decrypts its hexadecimal response with AES-128-CBC/PKCS7. A request without the player Referer returned `Request is invalid`; adding the root page Referer returned the encrypted metadata. No WebView or account was required.

The published HTTPS protocol uses key `kiemtienmua911ca` and IV `1234567890oiuytr`. These are public player obfuscation constants, not account credentials. Kotlin uses the same AES operation directly, rather than executing remote JavaScript. The implementation has a strict hexadecimal parser and a bounded accepted payload length.

The decrypted API response supplies a signed `cfNative` playlist on the embedding origin. Prefer it to the separate direct `source`: the browser injects tokens into direct source children, whereas native HLS includes those tokens in its playlists already. The extractor validates matching `swarmId`, the native provider URL, its token parameters and a successful HLS response. It reports API errors, invalid ciphertext and HTML placeholders instead of returning them as video sources.

## Live evidence

- Signed native master: HTTP 200, HLS with two variants (1280×640 and 1920×960).
- Variant: HTTP 200, HLS with an MP4 initialization map and signed segment URLs.
- Bounded segment range: HTTP 206, 1,024 bytes.
- Actual Kotlin extractor plus master/variant and three-second FFmpeg video/audio decode: passed in 8,840 ms total; decoder exited zero with an empty error log.

The provider names the MP4 initialization and segments `.woff`/`.woff2`. FFmpeg's extension policy rejects those paths by default; explicitly permitting them in the live test allowed successful decoding. This does not disable certificate verification. URLs and playback token values are intentionally omitted from this report.

## Reproduction

The module README contains JVM/iOS simulator and optional live test commands. For the connected Android sample:

```sh
./gradlew :sample:androidApp:connectedDebugAndroidTest \
  '-Pandroid.testInstrumentationRunnerArguments.class=com.ead.project.moongetter.RpmstreamPlaybackTest' \
  '-Pandroid.testInstrumentationRunnerArguments.rpmUrl=https://pelisplus.rpmstream.live/#b1lamw'
```

This test uses the sample's OkHttp client, the actual extractor, and Media3 with the returned headers. It passed on Samsung SM-S901B / Android 16: initial rendered frame including extraction was 11,058 ms. Frames after seeking to ten minutes, near the end, and back to zero took 3,401 ms, 3,136 ms and 1,437 ms respectively. Seek timings include a one-second observation delay. Playback was muted, and the player was released after validation.

Six deterministic tests cover portable URL matching, an independently generated Node AES vector, signed URL preservation, wrong video/host rejection, the complete two-request extraction flow, placeholder rejection and API rate limits. JVM and iOS simulator runs pass. Android samples and desktop/iOS targets compile.

Signed URLs expire. Re-extract instead of persisting an old source. The current protocol depends on the provider continuing to offer native HLS; no fallback returns a direct source requiring browser token rewriting. Full-length decode, audible output, iOS AVPlayer playback, and token-expiry recovery remain separate acceptance checks.
