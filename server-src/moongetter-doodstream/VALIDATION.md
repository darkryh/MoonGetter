# Doodstream / Dsvplay validation

The factory recognizes `https://dsvplay.com/e/j0jlmfjfalnj` alongside the existing flexible Doodstream domain family and `d-s.io`.

## Android connection crash

The supplied Android log reports `javax.net.ssl.SSLHandshakeException: connection closed`, caused by EOF during the TLS handshake. This happens before an HTTP response, HTML parsing, or media extraction. Increasing the request timeout cannot repair a connection the endpoint closes immediately.

Doodstream now maps transport IO failures to `InvalidServerException` with `EMPTY_OR_NULL_RESPONSE`, which the sample handles as a provider response failure. Coroutine cancellation propagates normally. Sample result helpers also handle IO failures from other extractors.

On October 9, 2026, the supplied endpoint closed connections from curl, the browser, and the real Ktor CIO extractor. Forcing TLS 1.2/HTTP 1.1 did not change the outcome. No live HTML or media source was retrieved, so live playback support is not confirmed. The network evidence does not determine whether the server, an intermediary, or an access restriction causes the closure.

## Resolution workflow

The factory retains the flexible legacy Doodstream regex and recognizes `d-s.io`, `dsvplay.com`, and the observed `playmogo.com` route. The resolver first requests the supplied URL. If it fails or returns no usable player, it tries the same video ID through `https://doodstream.com/e/<id>` once. The HTTP client follows redirects; the resolver requires the final URL to retain HTTPS and the same `/e/<id>` path. It does not hardcode the rotating destination. Cancellation propagates without fallback.

Each candidate receives its own Origin and Referer. After resolution, the redirected embed supplies the Origin and Referer for the player endpoint and media request. Verification HTML is reported explicitly; the sample displays a verification-required message for the observed HTTP 403 response. It does not report the video as taken down.

No routing or signed-source cache is used. Failed routes and verification responses must remain retryable, and a signed media URL may expire. A future route cache should only store a successfully validated player route, with bounded lifetime and invalidation on failure.

## Request flow fixes

The extractor uses a scheme-and-host Origin and a valid embed Referer, follows the final response URL for the `/pass_md5` request, and parses multiline scripts with portable `[\s\S]` regexes. It makes two requests rather than fetching the embed a third time. It returns request headers to the player instead of server response headers.

The media URL concatenates the `/pass_md5` response, ten random alphanumeric characters, the player's token suffix, and an expiry timestamp. It uses milliseconds for `Date.now()` and seconds when the script explicitly divides the time by 1000. This request construction is covered by synthetic fixtures; it is not validated against this currently inaccessible embed.

## Repeatable checks

Portable regression checks:

```sh
./gradlew :moongetter-doodstream:jvmTest :moongetter-doodstream:iosSimulatorArm64Test
```

Tests cover domain recognition and spoof rejection, complete synthetic extraction both directly and after canonical fallback, redirected media request headers, multiline parsing, timestamp units, connection failure conversion, cancellation, mismatched video redirects, verification pages, and retry after a failed resolution. These checks run on JVM and the iOS simulator; both Android sample compilations are also checked.

Live extraction and initial media request:

```sh
MOONGETTER_DOODSTREAM_TEST_URL='https://dsvplay.com/e/j0jlmfjfalnj' \
./gradlew :moongetter-doodstream:jvmTest --rerun
```

The optional live test requires extraction and a successful media range request; it fails when the endpoint cannot be reached. It uses TLS verification and a 60-second request timeout. Results are in `build/test-results/jvmTest/TEST-com.ead.lib.moongetter.doodstream.DoodstreamLiveTest.xml`. Signed URLs are not logged. Without its environment variable, this test performs no network requests.

Once the endpoint responds, complete validation still requires a real source, media bytes, and Android first frame/audio/seek checks. An unavailable connection must display an error rather than terminate the application.

## Follow-up connection checks

A repeated live extraction of the supplied Dsvplay URL failed after 650 ms before receiving HTML. The user independently confirmed Firefox's `PR_END_OF_FILE_ERROR` on their connection. The `www` variant also failed TLS.

The original provider entry `https://doodstream.com/e/j0jlmfjfalnj` redirected to `https://playmogo.com/e/j0jlmfjfalnj`. Browser inspection initially showed a Doodstream player with the matching video title, but subsequent navigation went blank. Ordinary HTTP requests to the redirected player returned HTTP 403 with a Cloudflare verification page. The canonical redirect therefore identifies a current provider route but does not establish working browser-free extraction. The canonical fallback is now enabled with the identity and response checks described above. The real Ktor live test reached `playmogo.com` in 1,700 ms and failed with the explicit verification-required HTTP 403 error. This validates route discovery and error handling, not source extraction. No real media URL or playback was validated for this video. A longer timeout, parsing regex, or route cache cannot turn verification HTML into player HTML. Browser-free extraction still requires a supported provider verification/session flow; the current implementation does not complete that challenge.
