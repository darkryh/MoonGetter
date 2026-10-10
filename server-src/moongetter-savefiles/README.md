# Savefiles

`moongetter-savefiles` supports `https://savefiles.com/e/<code>` embeds, with optional `www`, trailing slash, query, and fragment. It is registered in `serverBundle`. Paste this supplied URL into a platform sample: `https://savefiles.com/e/riku8n8bps1j`.

This is a separate provider module. It reuses the shared playlist utility and JS unpacker but follows Savefiles' own form flow. The inspected player is plain JW Player configuration, so unpacking is not required for this URL.

## Extraction

The embed initially shows a poster/play placeholder whose form targets `/dl`. Submit a form-encoded POST with `op=embed`, `file_code=<code>`, `auto=1`, and an empty `referer` field; the HTTP Referer header remains the embed URL and Origin is the site origin. Parse the returned player's `sources` configuration, retaining the signed HLS or MP4 URL. The returned video request uses the player response URL as its Referer.

The extractor sends this POST directly and does not fetch the placeholder first. During validation, direct GETs of the embed returned a Cloudflare HTTP 403 challenge while the embed POST returned HTTP 200 with a source. No WebView or JavaScript execution is used by this module. If the form endpoint itself returns a Cloudflare challenge, extraction reports verification required rather than claiming the video was taken down.

The library/sample request timeout is 60 seconds. CDN URLs are temporary: extract fresh media for playback and retain the returned headers.

## Validate

```sh
./gradlew :moongetter-savefiles:jvmTest :moongetter-savefiles:iosSimulatorArm64Test
```

Shared tests cover domain matching, multiline configuration, signed query preservation, escaped URLs, unsupported source rejection, and challenge recognition. Regex uses `IGNORE_CASE` and `[\s\S]` for multiline content, with no JVM-only dot-all option.

For the actual provider and media requests:

```sh
MOONGETTER_SAVEFILES_TEST_URL='https://savefiles.com/e/riku8n8bps1j' \
./gradlew :moongetter-savefiles:jvmTest --rerun
```

The optional live test calls the real factory with Ktor CIO and TLS verification enabled. It requires an extracted source, successful HLS master and variant responses, and a successful initial media range request. Without the environment variable, it makes no network calls. Signed URLs are not logged.

A live run on October 9, 2026 completed extraction plus the master, variant and initial media checks in 2.1 seconds; the media range request returned HTTP 206. This does not measure first-frame playback. Rebuild the Android sample and use `MoonGetterPlayer` logs to validate first frame, audio and seeking on your device.
