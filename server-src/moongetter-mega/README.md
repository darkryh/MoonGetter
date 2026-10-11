# MEGA local streaming

`moongetter-mega` resolves public MEGA `/file/<handle>#<key>` links to a session-scoped HTTP URL on the playback device. It is registered through `MegaFactory` in `serverBundle`. It runs Ktor CIO on loopback, fetches encrypted HTTPS ranges using a dedicated binary client, and decrypts them in memory with AES-CTR. No WebView, account, Premium token, HLS conversion, shared directory or full-file download is needed.

Public folder links are also supported:

- `/folder/<public-handle>#<folder-key>` automatically selects a video only when the folder tree contains exactly one video. Other non-video files may coexist.
- `/folder/<public-handle>#<folder-key>/file/<node-handle>` selects that exact video, even if the folder contains multiple videos.
- An optional `/folder/<subfolder-handle>` before the file selector restricts selection to that subfolder. A subfolder link with exactly one video can resolve automatically.

Ambiguous folders, absent/non-video selections, unreadable candidate keys, and disconnected trees produce an extraction error. Folder listing is limited to 4 MiB and 10,000 nodes. File content still passes the existing decrypted MP4/EBML header check. Unsupported containers fail that check rather than being returned as a playable URL.

Folder node keys are decrypted locally using the public folder key, following [MEGA's node decryption implementation](https://github.com/meganz/webclient/blob/master/nodedec.js). Tickets use the selected node handle with the public folder context on both initial resolution and refresh. The folder key is discarded after resolution; only the selected file key remains in the playback session. Ticket metadata must match the selected filename and size.

## Design and validation plan

1. Parse the public handle/key strictly, request a fresh download ticket without sending the fragment key, and decrypt the filename locally.
2. Bind `127.0.0.1:0` and obtain the OS-assigned port. Allocate a cryptographically random 192-bit session path. Verify HEAD length and a decrypted MP4/EBML header through the real local URL before returning it.
3. Implement GET/HEAD, byte ranges including suffix/open-ended ranges, Content-Length/Content-Range, and HTTP 416 for invalid/multiple ranges. Serve decrypted data without converting ciphertext to strings.
4. Fetch fixed 1 MiB encrypted blocks, cache at most two decrypted blocks per session, and stream with backpressure. Allow at most four sessions and four body writers. Cancel upstream work when a session closes or its reader is cancelled. Clear keys and cached plaintext on release.
5. Refresh an expired ticket once on HTTP 403/404/410, reject changed metadata, and report quota/rate limits as HTTP 429. Reject truncated/oversized upstream data and unsupported striped tickets.
6. Test crypto independently, fault cases, actual localhost serving on JVM and iOS simulator, occupied-port selection, live range/seek behavior, and full desktop decoding. Compile Android and all iOS targets. Actual device first-frame, audio, pause/resume, seeking, and background lifecycle need device verification.

## Usage

Normal MoonGetter extraction returns a `Video` whose request URL points to localhost. That URL works only on the device/process that owns its playback session.

For explicit ownership:

```kotlin
val playback = MegaProxy.open(publicLink, timeout = 60_000)
// Give playback.url to your media player.
// Stop/release the player before releasing its URL:
MegaProxy.release(playback.url)
// At application shutdown:
MegaProxy.closeAll()
```

`releaseAsync(url)` is available for disposal callbacks. The shared sample releases selected sessions on player disposal and its remaining URLs on ViewModel cleanup. Sessions expire after ten minutes of inactivity; reads keep them alive. The idle maintenance task also stops an empty listener. Applications must release sessions explicitly instead of depending solely on expiry.

Android must permit cleartext HTTP for the loopback host (the samples already permit it). The iOS sample enables `NSAllowsLocalNetworking`. JVM/Android upstream uses OkHttp/JDK crypto; iOS uses Darwin/Apple crypto. CIO serves the loopback endpoint and validates it over plain HTTP. The local probe allows sixty seconds for the first upstream block, with a five-second local connection timeout.

OkHttp supplies normal DNS address fallback for JVM/Android. On the tested Android network, CIO failed immediately with `SocketException: Permission denied` when the API resolved IPv6 addresses before IPv4. Switching the upstream engine to OkHttp allowed playback without hardcoding IPs or changing certificate verification. See [OkHttp's connection recovery behavior](https://github.com/square/okhttp/blob/master/README.md).

## Validation commands

```sh
./gradlew :moongetter-mega:jvmTest :moongetter-mega:iosSimulatorArm64Test
```

The common tests verify independent AES-CBC/CTR vectors, block-offset decryption, range syntax, expired-ticket retry, cache reuse, quota handling, truncated input, cancellation/key clearing, and an actual local HTTP server with a synthetic upstream. The optional JVM live test does nothing without its environment variable.

```sh
MOONGETTER_MEGA_TEST_URL='<public MEGA file link>' \
MOONGETTER_MEGA_FFMPEG=1 MOONGETTER_MEGA_FULL_DECODE=1 \
./gradlew :moongetter-mega:jvmTest --tests '*MegaLiveTest*' --rerun
```

This checks the actual returned localhost URL, unaligned/suffix ranges, invalid ranges, missing routes, session release, an occupied socket on another port, beginning and ten-minute seeking, and optionally full video/audio decoding with FFmpeg. FFmpeg must be installed for decoder checks. Tests do not print the MEGA key or upstream ticket. File data is streamed to the null decoder output rather than saved as a video file.

An Android device test is available in the Compose sample:

```sh
./gradlew :sample:androidApp:connectedDebugAndroidTest \
  '-Pandroid.testInstrumentationRunnerArguments.class=com.ead.project.moongetter.MegaPlaybackTest' \
  '-Pandroid.testInstrumentationRunnerArguments.megaUrl=<public MEGA file link>'
```

It checks a rendered first frame, seeks to ten minutes, near the end, and back to zero, then releases the player and local session. It mutes playback; this is not a listening test.

## Limits

A localhost proxy adds a socket hop and does not remove MEGA bandwidth quotas. The tested sample has its MP4 index at the end, requiring tail reads during probing/seeking. Transfer latency remains provider/network dependent. A decrypted block cache is bounded to 2 MiB per session; each in-flight writer uses bounded additional buffers. A missing/expired session returns HTTP 404; an upstream failure after response headers closes the stream rather than pretending playback completed.

Current support is public files and unambiguous/explicitly selected folder videos with one download endpoint; password-wrapped links, striped/RAID tickets, codec conversion, casting to another device, and MEGA's full-file MAC verification are not implemented. Full decoding is a playback-content check and is not a cryptographic authenticity check. The loopback URL is private session state and must not be persisted or shared.
