# Fireload validation

Sample checked on 2026-10-09: [So Big! (1932), HDrip 480p MP4](https://www.fireload.com/5cab2fd19d300d84/So_Big!_(1932)_(HDrip_480p).mp4). The page identifies the file as 651.55 MB. Its full contents have not been reviewed.

## Reproduced workflow

The public file page contains `window.Fl = {dlink, dwait, dtext}` as JSON. Its timer controller copies `dlink` to the download button when `dwait` reaches zero. For this sample the wait is zero; there is no need to execute the timer or other page scripts.

A fresh page request establishes a `filehosting` guest cookie. Resolving the page's `pt` download ticket without that cookie returns HTML. A fresh ticket with the guest cookie returned HTTP 302 to `srv1.fireload.com`, then actual MP4. A bounded GET returned HTTP 206, `Content-Range: bytes 0-1023/683201696`, and an MP4 `ftyp` header. The resolved URL also responded to HEAD without the guest cookie. Reusing a consumed page ticket returned HTML.

The extractor therefore requests the page, verifies its file ID, parses bounded JSON, honors the provider wait (supported range 0–60 seconds), and supplies the guest cookie when resolving the ticket. KtorMoonClient maps a bodyless GET to HEAD. The returned request uses the final HTTPS media URL on a matching `srv<number>.fireload.com` file path, with canonical Referer/Origin. The guest cookie is not included in playback headers. Signed queries are preserved.

Ktor's HttpCookies plugin strips explicit Cookie headers when the configured storage is a no-op. Live validation must use cookie storage, as both platform samples do. Disabling cookies produces an unresolved ticket; this is not evidence that the resource was taken down.

## Checks

```sh
./gradlew :moongetter-fireload:jvmTest :moongetter-fireload:iosSimulatorArm64Test :sample:shared:compileKotlinIosArm64
MOONGETTER_FIRELOAD_TEST_URL='https://www.fireload.com/5cab2fd19d300d84/So_Big!_(1932)_(HDrip_480p).mp4' MOONGETTER_FIRELOAD_FFMPEG=1 ./gradlew :moongetter-fireload:jvmTest --rerun-tasks
SIMCTL_CHILD_MOONGETTER_FIRELOAD_TEST_URL='https://www.fireload.com/5cab2fd19d300d84/So_Big!_(1932)_(HDrip_480p).mp4' ./gradlew --no-daemon :moongetter-fireload:iosSimulatorArm64Test --rerun-tasks
```

Four common regression tests cover configuration/wait bounds, matching IDs and provider hosts, session-cookie forwarding, signed-query preservation, caller-header immutability, final-source selection, unresolved tickets, HTML placeholders, and foreign redirects. The optional JVM live test resolves a real ticket and decodes three seconds from the start and after seeking to 600 seconds, rejecting empty decoder output. The optional iOS live test uses Darwin and IosCookieManagement to resolve the actual guest ticket and validate the final source.

## Results and iOS limitation

JVM live extraction and both decode/seek checks passed in 31.63 seconds total. All four common tests passed on JVM and the iOS simulator. The signed iPhone build succeeded and the updated sample was installed on the connected iPhone.

The live Darwin simulator test ran when the URL was forwarded through `SIMCTL_CHILD_MOONGETTER_FIRELOAD_TEST_URL`. It failed before the first HTTP response with NSURL error -1202. Apple's trust evaluator returned OSStatus -26276 with TrustResultValue 0. A fresh Gradle process reproduced the failure. The simulator clock was correct, the certificate covers fireload.com and *.fireload.com and is valid from 2026-09-28 to 2026-12-27, and a separate macOS URLSession HEAD request succeeded with HTTP 200. These checks do not establish the exact cause of the simulator trust failure or physical iPhone connectivity. Certificate validation remains enabled; no trust bypass was added. Logs: `/tmp/fireload-darwin-fresh.log` and `/tmp/fireload-darwin-trust.log`.

The module has moved from `server-robot-src` to `server-src`, and both file-host factories now live in the regular bundle. The Robot API and modules have been removed without a compatibility layer. Both sample apps start with the Fireload URL above.

Native parser/network tests and a signed device build do not establish physical iPhone AVPlayer playback. Test playback and seeking in the updated iPhone sample separately. The extractor requires the current public configuration; it does not silently fall back to WebView when the provider changes it.
