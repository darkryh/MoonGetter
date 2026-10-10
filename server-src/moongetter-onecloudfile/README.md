# 1CloudFile

HTTP-only Kotlin Multiplatform extractor for `https://1cloudfile.com/<id>` video links. It belongs to `moongetter-server-bundle`; it uses HTTP requests without a browser or JavaScript/WebAssembly runtime.

The current site embeds its signed download URL as hex bytes XOR-encoded with `0x7a`. The extractor decodes the download-link argument, accepts HTTPS video URLs on `fs<number>.1cloudfile.com`, and checks the ticket with a bodyless request (HEAD in KtorMoonClient). It returns the original signed URL with Referer/Origin headers. An unsupported encoding, unexpected redirect, HTML response, or unsuccessful ticket request produces an extraction error.

Use `OneCloudFileFactory` directly or the regular `serverBundle`. The module coordinate and factory package are unchanged.

Do not cache signed URLs indefinitely. Extract a fresh URL and pass the returned request headers to the player. HTTP-only extraction does not make an unavailable file accessible.

See [validation](../../docs/validation/onecloudfile.md) for the sample and checks.
