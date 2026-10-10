# MEGA public-file validation — October 9, 2026

The user-supplied public file handle was 8RBl3ZQR. Its fragment key was used locally for this validation and is not copied into this report.

## Initial feasibility probe

A body-only JSON POST to https://g.api.mega.co.nz/cs?id=1 with `[{"a":"g","g":1,"p":"8RBl3ZQR"}]` returned a download ticket and encrypted attributes. File size: 308,096,465 bytes. Decrypted filename: `02-Tokyo3.mp4`.

The API initially supplied an HTTP endpoint; the same endpoint over HTTPS worked. Two bounded POST range requests followed the official webclient's `/start-endInclusive` path convention:

- 0–4095: HTTP 200, 4096 bytes, approximately 590 ms.
- 112–255: HTTP 200, 144 bytes, approximately 566 ms.

Encrypted bytes had no MP4 `ftyp` signature. After AES-CTR decryption, the first range had the correct signature. The independently decrypted second range exactly matched the corresponding bytes in the first range.

No account, Premium token, WebView, or JavaScript engine was required. Only 4240 encrypted file bytes were fetched. This confirms metadata/key compatibility and byte-range decryption, not full playback, full-file integrity or transfer-quota availability over an entire session.

## Protocol and integration

Primary reference: https://github.com/meganz/webclient/blob/master/js/utils/network.js

Decode the URL-safe Base64 fragment into 32 bytes. The AES-128 key is the XOR of the first 16 bytes with the last 16. File attributes use AES-CBC with a zero IV, zero padding and the `MEGA` prefix before JSON. Data uses AES-CTR: the first eight counter bytes are fragment bytes 16–23; the final eight are the big-endian block index. Align a requested offset down to a 16-byte boundary and discard the leading alignment bytes after decryption.

A ticket URL is an encrypted data source, not a directly playable URL. MoonGetter must not register it as an ordinary Video source. Reliable streaming requires a binary-safe range transport and a player adapter that decrypts on demand (for example a Media3 DataSource on Android). The current MoonClient response exposes a string body; ciphertext must never be passed through UTF-8 conversion. iOS needs an equivalent resource-loading adapter or a suitable local range proxy. The new module uses a dedicated binary transport and a Ktor CIO loopback proxy, which lets existing players request a normal HTTP URL. The original MoonClient string-body contract is unchanged.

Production acceptance must cover initial frame, audio, unaligned and backward seeks, cancellation, bounded memory, ticket refresh, transfer quota errors, authenticated full-file integrity where applicable, and actual Android/iOS device playback. MEGA is now registered through `MegaFactory`; see [the implemented proxy and validation plan](../../server-src/moongetter-mega/README.md). The earlier range probe below established feasibility before that implementation. Full-file decoding is checked through the loopback proxy without saving a full video file.

Keep the fragment key local; send only the public handle to the metadata API. Do not cache expiring tickets indefinitely or print keys/ticket URLs in diagnostics.

## Local proxy implementation

The module binds only to `127.0.0.1` using port zero, reads the actual assigned port, and verifies HEAD plus a decrypted media-header range through the local session URL before returning it. It uses cryptographically random session paths, fixed 1 MiB upstream blocks, and at most two cached plaintext blocks per session. It supports GET/HEAD, unaligned/open-ended/suffix ranges, HTTP 416, ticket refresh, quota mapping, cancellation, key clearing, expiry, and explicit shutdown. No shared filesystem directory is needed.

JVM and iOS simulator tests include the actual local server, along with independent crypto vectors and synthetic upstream failures. Both Android samples compile, and iOS ARM64 device and Apple Silicon simulator targets compile. Paste the supplied public MEGA link into a platform sample. The sample releases local sessions when replacing a playlist or clearing its ViewModel.

An earlier desktop live run validated real decrypted ranges, suffix/unaligned seeks, release, and beginning/ten-minute-seek FFmpeg decoding. It took about 117 seconds using the initial smaller block implementation; this is an aggregate test time, not a first-frame measurement. Subsequent buffering uses 1 MiB blocks after bounded range measurements of approximately 1.55 seconds for 256 KiB and 2.39 seconds for 1 MiB.

### Final desktop live test

The final optional JVM live test passed with FFmpeg configured to fail on decoding errors. It streamed through the exact `MegaProxy.open()` URL and decoded the entire 308,096,465-byte public video, including its selected video and audio streams, without saving a media file. FFmpeg exited zero and its error log was empty.

- Opening three-second decode: 5,377 ms.
- Ten-minute seek followed by three-second decode: 7,672 ms.
- Full video/audio decode: 299,434 ms (about five minutes).
- Complete live test, including extraction, range checks and cleanup: 317,410 ms.

These are decoder/test timings on this connection, not player first-frame guarantees. The test also passed unaligned and suffix ranges, HTTP 416 for an invalid range, missing-route HTTP 404, occupied-port selection, and HTTP 404 after releasing the session. Final server shutdown completed without an error. The full MP4 decode demonstrates that the proxy can traverse the complete file; it does not authenticate MEGA's file MAC.

### Android device playback

The real Compose instrumentation test passed on a Samsung SM-S901B running Android 16. It opened the supplied public file through `MegaProxy`, rendered with Media3/ExoPlayer, and completed three seeks. Initial rendered frame, including extraction: 16,979 ms. Rendered frames after seeking to ten minutes, near the end, and back to zero: 4,512 ms, 9,731 ms, and 2,142 ms respectively. Seek timings include a one-second playback observation delay. The test released the player, session and listener afterward.

The initial CIO upstream client failed before player preparation with `SocketException: Permission denied`. DNS returned IPv6 addresses first, followed by IPv4. Switching only the JVM/Android upstream transport to Ktor OkHttp resolved this on the same device/network; this is consistent with connection address fallback, rather than an extractor or decryption failure. [OkHttp documents alternate-address recovery](https://github.com/square/okhttp/blob/master/README.md). TLS certificate verification remains enabled and no IP addresses are hardcoded.

The startup localhost probe now allows sixty seconds for the first media block; its connection timeout remains five seconds. This avoids rejecting a healthy local listener simply because MEGA takes more than five seconds to serve the first block.

Nine deterministic tests passed on both JVM and iOS simulator (plus the optional JVM live test, which performs no network work without its environment variable). Android samples and desktop sample compile, as do iOS Arm64/X64 targets. Actual iOS AVPlayer playback, audible output, background behavior and larger-scale concurrency remain acceptance checks. The Android test was muted. MEGA full-file MAC authentication is not implemented; decoding checks media readability, not cryptographic authenticity.

## Public folder selection

The supplied public folder handle `DGYnRKRZ` returned 27 nodes: one root folder and 26 MP4 files. The bare folder link therefore fails with a selection-required error; it does not return an arbitrary video. A folder with exactly one video resolves automatically, including when non-video files coexist. Explicit `/file/<node-handle>` selectors work in multi-video folders, and optional subfolder selectors restrict the eligible subtree.

Seven additional common tests passed on JVM and iOS simulator. They cover folder/selected-file parsing, an independent AES-ECB node-key fixture, single-video selection, ambiguous-folder rejection, subfolder scope, missing/non-video/unreadable selections, folder-scoped ticket renewal, and rejection/key cleanup when selected metadata changes. The public folder key is cleared after resolution, unused decrypted file keys are cleared, and the selected key is cleared when its session closes. Tickets and renewed tickets must match the selected file's filename and size.

The live test selected node `zTRxVCQT` as an explicit test case (not as the default choice for bare links). It verified rejection of the original ambiguous folder and streamed the selected 218,979,995-byte file through the exact returned loopback URL. Initial three-second video/audio decode passed in 4,983 ms; a ten-minute seek and three-second decode passed in 16,992 ms. Aggregate extraction/range/seek/release test time was 28,383 ms. FFmpeg exited zero with an empty error log. Unaligned/suffix reads, occupied-port selection, invalid ranges, missing routes and session release passed as well. This folder file was checked with short decode and seek tests; its entire file was not decoded during this run.

Paste the explicit selected-file link into a platform sample for testing. The original folder remains ambiguous; pass a URL ending in `/file/<node-handle>` to choose a particular video. Primary protocol reference: [MEGA node decryption](https://github.com/meganz/webclient/blob/master/nodedec.js).

The selected folder file also passed the Android Media3 device test on Samsung SM-S901B / Android 16. Initial rendered frame including folder resolution: 16,533 ms. Rendered frames after ten-minute, near-end and beginning seeks: 5,888 ms, 10,007 ms and 4,114 ms, including a one-second observation delay per seek. Playback was muted; player/session cleanup completed successfully. Both Android samples and the iPhone Kotlin source compile. Actual iOS AVPlayer playback of this folder file remains untested.
