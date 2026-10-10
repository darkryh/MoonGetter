# Validate Filemoon / Byse extraction

The supplied mirror `https://bysekoze.com/e/y6kmotbkxiha/` serves a Byse JavaScript application. Its HTML contains no legacy player iframe or packed HLS configuration. The extractor recognizes this shell and uses its HTTP verification/playback API.

## Browser-free flow

1. POST an empty JSON object to `/api/videos/{code}/embed/captcha`.
2. Compute the work solution in common Kotlin, off the UI thread. `ByseProofOfWork` bounds difficulty, attempts and elapsed time, and yields for cancellation.
3. POST the work token and solution to `embed/captcha/verify`; require `status: ok` and a nonempty verification token.
4. POST `{"fingerprint":{"token":"","viewer_id":"","device_id":""}}` to `embed/playback`, with the verification token in `X-Captcha-Token`. The inspected API accepts this empty fingerprint envelope. Empty POST bodies and GET requests returned HTTP 405, even with valid verification tokens.
5. Authenticate and decrypt the returned `playback` using AES-GCM. Select the key parts using the frontend's version mapping and decode Base64 URL values. Authentication failures must not produce a resource.
6. Return the decoded media source with the embed Referer. Preserve existing legacy packed/iframe behavior for older Filemoon pages.

The API advertises `sha256-leading-zero-bits`, but the public player bundle uses a custom work function. A SHA-256 solution was rejected; the player's algorithm was accepted. The implementation follows the validated player algorithm. No JavaScript execution, browser fingerprint simulation, image CAPTCHA solver, or WebView is used by the extractor. This result applies to the inspected URL and flow; provider changes or other verification requirements may fail.

AES-GCM uses cryptography-kotlin 0.6.0's optimal provider (JVM JCA and Apple CryptoKit). First builds require network access to resolve its runtime dependencies.

## Portable tests

```sh
./gradlew :moongetter-filemoon:jvmTest :moongetter-filemoon:iosSimulatorArm64Test
```

Shared tests cover direct packed HTML, relative/multiline iframe parsing, Byse shell recognition, the work solver's first matching result and bounded failure, versioned key selection, AES-GCM decryption, and rejection of tampered ciphertext. Crypto fixtures are synthetic; they contain no real signed URLs or verification tokens.

## Live extractor and media validation

```sh
MOONGETTER_FILEMOON_PROTECTED_TEST_URL='https://bysekoze.com/e/y6kmotbkxiha/' \
./gradlew :moongetter-filemoon:jvmTest --rerun
```

This test invokes the real Filemoon factory with Ktor CIO, a 60-second per-request timeout, and TLS verification enabled. It requires a nonempty extracted source, fetches the master and first variant, validates HLS signatures, then requires a successful initial media request. Results and elapsed time are in `build/test-results/jvmTest/TEST-com.ead.lib.moongetter.filemoon.FilemoonLiveTest.xml`. Signed URLs and tokens are not printed.

To isolate verification and payload decryption:

```sh
MOONGETTER_BYSE_VERIFY_TEST_URL='https://bysekoze.com/e/y6kmotbkxiha/' \
./gradlew :moongetter-filemoon:jvmTest --rerun
```

Optional live tests do not perform network calls unless their environment variables are set. A portable test pass alone does not establish live provider access.

## Device acceptance

Paste the Byse URL into the Android sample. Rebuild it to include the new extractor and test playback. Record extraction elapsed time separately from the player's first frame. The sample logs player timing under `MoonGetterPlayer`.

HTTP playlist and media access does not prove decoding or rendering on an Android/iOS device. Validate first frame, audio, seeking, expired URL re-extraction, repeated cold/warm requests, cancellation, and UI responsiveness on actual devices. iOS simulator crypto tests validate Native decryption; the optional live network tests currently run on JVM.

A missing source maps to an extraction failure in the sample, rather than claiming the resource was taken down. Work-budget exhaustion is a timeout; verification rejection and malformed/authentication-failed payloads are extraction failures; HTTP errors retain their status code.
