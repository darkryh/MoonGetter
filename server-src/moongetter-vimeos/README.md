# Vimeos

The `moongetter-vimeos` module extracts the HLS master URL from packed JW Player configurations on `vimeos.net`. It reuses MoonGetter's Kotlin Multiplatform JS unpacker without executing JavaScript.

The factory matches `https://vimeos.net/embed-<id>.html`, optionally with `www`, a query, or a fragment. It is registered in `serverBundle`. Paste the supplied embed URL into any platform sample, or set `SampleConfiguration.initialVideoUrl`.

The extractor returns a `Video` with its request headers, including the embed Referer and site Origin. Pass those headers to your media player. CDN URLs contain temporary signatures; extract them again when testing.

Run offline regression tests and native validation from the repository root:

```sh
./gradlew :moongetter-vimeos:jvmTest :moongetter-vimeos:iosSimulatorArm64Test
```

Run extraction against the supplied page:

```sh
MOONGETTER_VIMEOS_TEST_URL='https://vimeos.net/embed-2nulhg4mnuz7.html' \
./gradlew :moongetter-vimeos:jvmTest --rerun
```

Add `MOONGETTER_VIMEOS_VALIDATE_MEDIA=1` to also require HTTP 200 and an HLS master playlist from the CDN. Set `MOONGETTER_VIMEOS_SOURCE_FILE` to an absolute file path to save the extracted source.

The supplied page yields an HLS source. Earlier checks returned HTTP 403, while a fresh extraction later returned HTTP 200 for the master, variant, and first video segment. Always extract a fresh signed URL and retain its request headers.

## Diagnose slow startup

The live extraction test prints page download, unpacking overhead, and total extraction times in `build/test-results/jvmTest/TEST-com.ead.lib.moongetter.vimeos.VimeosLiveTest.xml`. These measurements exclude playback buffering.

In Android Studio Logcat, filter on `MoonGetterPlayer` to see state transitions, errors, and elapsed time from media assignment to the first rendered frame. State 2 means buffering and state 3 means ready. Compare these timings with the extraction test on the same connection. HTTP failures or long media requests point to delivery or request headers; fast extraction with a slow first frame points to player loading, buffering, or decoding.

A measured run on October 9, 2026 took 792 ms to fetch the embed page, 16 ms for unpacking and overhead, 774 ms for the master playlist, 1.31 s for the video playlist, and 1.29 s for the first video segment. These are individual requests, not a measurement of first-frame time. The stream also has separate audio playlists, which the player must load.

Validate player buffering on the device separately from source extraction; reducing buffers without measurements can increase stalls.
