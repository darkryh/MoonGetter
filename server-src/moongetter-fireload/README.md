# Fireload

HTTP-only Kotlin Multiplatform extractor for public Fireload file pages. It is registered in `moongetter-server-bundle` and uses HTTP requests without browser or JavaScript execution.

Sample: [So Big! (1932), MP4](https://www.fireload.com/5cab2fd19d300d84/So_Big!_(1932)_(HDrip_480p).mp4). The filename identifies the film; its full contents have not been reviewed.

The extractor reads the page's `window.Fl` JSON, honors its download wait, retains the guest `filehosting` cookie, and resolves `dlink` with a bodyless request. It validates the final matching HTTPS file-server URL and content type, then returns that resolved URL with playback headers. It reports an extraction error for unsupported configuration, an unresolved ticket/HTML response, or a mismatched file redirect.

Use cookie storage with KtorMoonClient. Ktor's HttpCookies plugin removes supplied Cookie headers when its storage is disabled; `MoonCookie.Management.newEmptyFactory()` cannot retain this guest session. The Android/JVM sample uses `JavaNetCookieManagement`; iOS uses `IosCookieManagement`. The live tests also retain cookies.

Download page tickets can be single-use. Resolve a fresh ticket for each extraction, and give the player the returned final source and headers. Do not give the player the original `pt` ticket or reuse it as a cached stream URL. No premium account is used in this workflow.

Use the regular server bundle or register `FireloadFactory` directly. The module coordinate and factory package are unchanged.
