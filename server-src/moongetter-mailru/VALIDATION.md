# Mail.ru embed validation

Validated October 9, 2026 against `https://my.mail.ru/video/embed/7512958666216054522#aylaz9ymde#7930`.

## Workflow

The new `moongetter-mailru` KMP module and `MailruFactory` are registered in the server bundle. Both Android samples and the shared sample input contain this URL.

1. Fetch the HTTPS embed without its fragment. The fragment is browser navigation data and is not needed by this endpoint.
2. Read `metadataUrl` from the embed configuration. Accept only the same video's `/+/video/meta/<id>` endpoint on `my.mail.ru` over HTTPS.
3. Read `videos` from JSON, require the matching metadata ID, preserve quality and signed query parameters, and normalize protocol-relative URLs to HTTPS.
4. Return media request headers including the embed Referer and the metadata response's `video_key` cookie. Do not forward Set-Cookie or unrelated response headers.

No WebView, JavaScript execution, login or Premium token is used. Each extraction requests fresh metadata; signed URLs/cookies are not cached indefinitely. Current scope is numeric Mail.ru embed URLs, not arbitrary profile/watch-page URLs.

## Live evidence

The embed and metadata returned HTTP 200. Metadata described `4050_1.mp4`, duration 5430 seconds, with 720p and 360p MP4 variants. Bounded 1024-byte range requests for both variants returned HTTP 206, `video/mp4`, and an MP4 `ftyp` signature. Their initial range requests took approximately 710–730 ms on this connection.

The real Ktor extractor independently returned two sources and successfully requested initial media bytes (HTTP 206), taking 4595 ms for extraction plus the media request. This is not a first-frame measurement. Actual Android/iOS playback, audio and seeking still require device validation.

## Repeatable checks

```sh
./gradlew :moongetter-mailru:jvmTest :moongetter-mailru:iosSimulatorArm64Test
```

Four common tests cover domain/metadata spoof rejection, quality and signed-query preservation, cookie isolation, and the complete two-request extraction. They run on JVM and the iOS simulator. Both Android sample compilations are checked separately.

```sh
MOONGETTER_MAILRU_TEST_URL='https://my.mail.ru/video/embed/7512958666216054522#aylaz9ymde#7930' \
./gradlew :moongetter-mailru:jvmTest --tests '*MailruLiveTest*' --rerun
```

The optional live test requires sources and a successful initial media request; without the environment variable it makes no network requests. It uses TLS verification and a 60-second request timeout, and does not log signed URLs or cookies.
