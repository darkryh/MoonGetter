# GoFile validation — October 9, 2026

Target: https://gofile.io/d/zcIJuTK3

The public page returns an application shell rather than media URLs. Current official frontend modules show this workflow:

1. POST https://api.gofile.io/accounts creates a guest account token.
2. The frontend loads /js/wt.obf.js and generates a website token from the account token and browser context.
3. GET /contents/<contentId> uses Authorization: Bearer, X-Website-Token and X-BL, with pagination parameters.
4. Successful content responses contain file metadata/children; media links must then be validated separately.

Guest creation succeeded. Running the current generator in an isolated JavaScript context produced a 64-character website token. Listing the supplied folder returned HTTP 401/error-notPremium; a retry with matching user-agent headers returned HTTP 429/error-rateLimit. No further requests were made after rate limiting. These outcomes do not establish whether the folder is available or whether the generated website token would be accepted in a normal browser session. No media URL, media bytes, or playback was validated.

The official API page (https://gofile.io/api; current source /js/pages/api.js) marks GET /contents/{contentId} Premium-only and documents Bearer authentication. That is the supported path for a browser-free extractor. A valid Premium API token is needed to validate it end to end; do not embed that token in shared samples or log it.

Do not ship an anonymous extractor based on a hardcoded website token or copied obfuscated secret. The current frontend explicitly describes secret rotation and an app-age guard. Do not retry HTTP 429 in a tight loop, treat error-notPremium as file deletion, or cache failed responses as unavailable resources.

A future KMP module should accept caller-supplied Bearer authorization, validate content IDs, handle API errors distinctly, paginate folder children with bounded work, select media files, validate HTTPS media URLs, and pass only required media headers. Acceptance requires an actual authenticated listing and a bounded successful media range request, followed by Android/iOS playback checks. A GoFile module and factory are now registered. They implement the free website flow with the existing Rhino/JavaScriptCore engines, a fresh signing-script fetch, bodyless guest POST, and matching user-agent/language headers. No Premium token is created or required. The final real Ktor live test received error-rateLimit; source retrieval remains unverified. The extractor currently supports the first 100 immediate folder entries or a direct file response; nested/paginated folders are not traversed. Five portable tests cover parsing, gates, JavaScript context and the complete synthetic three-request flow. Paste the supplied URL into a platform sample.

Run regression tests with `./gradlew :moongetter-gofile:jvmTest :moongetter-gofile:iosSimulatorArm64Test`. Run live acceptance with `MOONGETTER_GOFILE_TEST_URL=https://gofile.io/d/zcIJuTK3 ./gradlew :moongetter-gofile:jvmTest --tests "*GofileLiveTest*" --rerun`. The latter must fail if no actual source/media is received. Signing scripts and session tokens are not cached or logged. The reused JS runtime does not provide a hard execution-time limit; a future shared-runtime redesign should add that bound before treating arbitrary future generator changes as safe to execute.
