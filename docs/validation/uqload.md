# Uqload validation

Validated on 2026-10-09 with `https://uqload.co/embed-pcmet1dsyefs.html`.

## Failure and workflow

The input redirects to the same embed ID on `uqload.vc`, with a query string. Its player configuration is Dean Edwards packed JavaScript and contains `sources: [{file: "...master.m3u8?..."}]`. The old extractor only recognized an unpacked string array. The resource was available; the parser missed it.

The extractor follows the client redirect, validates that the final URL is another Uqload embed with the same ID, unpacks the player configuration, and accepts HTTPS MP4/HLS source entries. It preserves signed query strings and returns the final embed as Referer plus its origin as Origin, without mutating caller headers. HLS sources are requested and checked for a nonempty HLS manifest before returning. HTML placeholders and unsuccessful responses report an extraction error. Both original and canonical headers returned the live master in this check; headers alone were not the reproduced cause.

The shared unpacker now leaves words outside its token dictionary unchanged rather than replacing them with token zero or indexing beyond the dictionary. Extraction uses common Kotlin regex options and requires no WebView or JavaScript execution.

## Repeatable checks

```sh
./gradlew :moongetter-uqload:jvmTest :moongetter-uqload:iosSimulatorArm64Test :sample:shared:compileKotlinIosArm64
MOONGETTER_UQLOAD_TEST_URL=https://uqload.co/embed-pcmet1dsyefs.html MOONGETTER_UQLOAD_FFMPEG=1 ./gradlew :moongetter-uqload:jvmTest --rerun-tasks
```

The optional network test fetches the actual master and variant and, with FFmpeg enabled, decodes three seconds of media. It passed in 7.45 seconds including extraction, playlist requests, and decode. Signed URLs are temporary: extract a fresh URL for each playback session and pass the returned request headers to the player. Paste this embed URL into a platform sample.

Common regression cases cover packed/object/string sources, multiline configuration, excluding posters/subtitles/invalid URLs, canonical redirect headers, signed queries, unrelated redirects, and HTML playlist placeholders. All five common cases passed on JVM and the iOS simulator, and the signed iPhone build succeeded. Native compilation and parser tests do not establish physical iPhone playback; test playback and seeking in the updated iPhone sample separately.
