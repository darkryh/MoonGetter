# 1fichier

Experimental HTTP extractor for public HTTPS file links such as [the supplied MP4](https://1fichier.com/?npic4u16rb5qucp7ug9w). Install `io.github.darkryh.moongetter:moongetter-onefichier` with your MoonGetter version and register `OnefichierFactory`, or use the regular server bundle. Both samples include this factory through that bundle; paste the link into the sample's URL field.

The free workflow reads the file metadata and same-file POST form, retains hidden fields, waits for the advertised countdown (0–60 seconds supported), requests inline delivery, extracts the provider's download anchor, and validates the final HTTPS media response with a bodyless request. KtorMoonClient uses HEAD for this validation. Persistent client cookie storage is needed if the provider establishes a session. No subscription or API key is created or purchased. Signed download URLs should be extracted afresh rather than cached indefinitely.

Free access has provider restrictions. Occupied guest slots, cooldowns, password requirements, and recognized interactive verification return explicit extraction errors. There is no automatic retry loop, concurrent download workaround, or browser fallback. Existing session cookies can be supplied through the configured client's cookie storage, but login automation is not implemented. Unknown layouts fail rather than guessing a source.

On 2026-10-09 the supplied page identified `4482_1_SUB.mp4`, 230.01 MB, and a 60-second guest countdown. The form response reported that all free guest slots were occupied and suggested free-account login. The provider advertises limited guest speed, no simultaneous guest downloads, and no guest resume support. Therefore reliable seeking and live playback are not confirmed for this sample. The source-anchor parser and successful media workflow are covered by synthetic regression responses, not a captured successful ticket from this link; treat support as experimental until a live ticket and playback can be tested.

Validation:

```sh
./gradlew :moongetter-onefichier:jvmTest :moongetter-onefichier:iosSimulatorArm64Test :moongetter-server-bundle:compileKotlinJvm :sample:shared:compileKotlinIosArm64
```

Tests cover same-file form scoping, countdown bounds, HTTPS download hosts, signed URL preservation, the form-to-media workflow, rejection of HTML media responses, and guest-capacity errors. They do not prove provider availability or player seeking support.
