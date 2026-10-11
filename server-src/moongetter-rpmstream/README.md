# RPMStream

`moongetter-rpmstream` resolves public HTTPS `rpmstream.live/#<videoId>` embeds, including provider subdomains such as `pelisplus.rpmstream.live`. `RpmstreamFactory` is registered in `serverBundle`; both samples contain the supplied test URL.

The provider's public player calls `/api/v1/video?id=...&w=1920&h=1080&r=` with its own root page as Referer. The response is hexadecimal AES-CBC ciphertext. The extractor implements that HTTPS player protocol in Kotlin Multiplatform, using JDK crypto on JVM/Android and Apple crypto on iOS. It needs no JavaScript engine or WebView.

The decrypted response contains a signed `cfNative` HLS URL. Its master and child playlists already carry the playback tokens, so ordinary HLS players can use it without the browser's custom token-injection loader. The extractor checks the video ID, HTTPS/provider host, required token parameters, and a successful `#EXTM3U` response before returning a `Video`. Preserve the returned request headers and complete signed URL when constructing the player request. Each extraction gets fresh metadata; do not permanently cache this expiring URL. Re-extract if playback tokens expire.

The current wire constants were verified against [the provider's published player script](https://pelisplus.rpmstream.live/assets/index-B82x0F06.js). An incompatible protocol change or missing native HLS endpoint produces an extraction error rather than returning an HTML placeholder or an incomplete direct source.

```sh
./gradlew :moongetter-rpmstream:jvmTest :moongetter-rpmstream:iosSimulatorArm64Test

MOONGETTER_RPMSTREAM_TEST_URL='https://pelisplus.rpmstream.live/#b1lamw' \
MOONGETTER_RPMSTREAM_FFMPEG=1 \
./gradlew :moongetter-rpmstream:jvmTest --tests '*RpmstreamLiveTest*' --rerun
```

The optional live test requires FFmpeg and checks the real extractor, master, variant and a short video/audio decode. FFmpeg needs `-allowed_extensions ALL -extension_picky 0` for this provider's `.woff`/`.woff2` media paths. These are MP4 media resources despite their names; this option is only part of the decoder test, not a TLS workaround.

Full-length playback, iOS AVPlayer playback and behavior after token expiry are not established by the short live test.
