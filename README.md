[![](https://jitpack.io/v/darkryh/MoonGetter.svg)](https://jitpack.io/#darkryh/MoonGetter)

# MoonGetter for Kotlin

[![App Icon](assets/images/moon-getter.png)](https://play.google.com/store/apps/details?id=com.ead.project.moongetter)
[![Play Store Icon](assets/images/play-store.png)](https://play.google.com/store/apps/details?id=com.ead.project.moongetter)

**MoonGetter** is a powerful and versatile Kotlin-first library focused on stream extraction and video hosting download logic. It supports Android, is progressively expanding into Kotlin Multiplatform (KMP), and allows seamless custom server integration.

---

## ✅ Features

- **Custom server integration** — Build and plug in your own streaming providers.
- **Coroutine-based architecture** — Async-first, highly scalable extraction logic.
- **Transparent HTTP client injection** — Works with both OkHttp and Ktor.
- **Custom error types** — Intuitive error flow through `InvalidServerException` and `Error` enums.
- **Flexible cookie management** — Transparent for OkHttp, configurable for Ktor.

---

## 🔄 Compatibility Table

| Feature                | Android | Kotlin | Kotlin Multiplatform |
|------------------------|:-------:|:------:|:--------------------:|
| Core Library           |    ✅    |   ✅    |       ⏳ (Planned)       |
| Robot Servers API      |    ✅    |   ⚠️    |       ⏳ (Planned)    |
| Server Implementations |    ✅    |   ✅    |       ⏳ (Planned)    |

> ✅ = Supported / ⚠️ = Limited / ⏳ = In development

---

## 🌐 Supported Servers (Core)

> ✅ Actively maintained / 💥 Deprecated

- ✅ Google Drive
- ✅ Mediafire
- ✅ Streamtape
- ✅ PixelDrain
- ✅ Okru
- ✅ StreamWish
- 💥 Voe *(Deprecated)*
- ✅ Senvid
- ✅ Vihide
- ✅ Hexload
- ✅ YourUpload
- ✅ Facebook
- ✅ XTwitter
- ✅ LuluStream
- ✅ Mp4Upload
- ✅ Uqload
- ✅ Mixdrop
- ✅ Doodstream
- ✅ Vidguard *(now part of core servers, no longer a robot)*
- ✅ Filemoon

---

## 🤖 Robot Servers *(Optional)*

> These require the `moongetter-core-robot` and a platform Robot API. Currently only Android is supported.

- 💥 Fireload *(Deprecated)*
- ✅ 1CloudFile

**Coming Soon:** GoodStream, Gofile, Abyss

---

## ☕ Installation (Gradle)

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Required core logic
    implementation("com.github.darkryh.MoonGetter:moongetter-core:$version")

    // Choose your HTTP client
    implementation("com.github.darkryh.MoonGetter:moongetter-client-okhttp:$version") // OkHttp (Default for Android)
    // OR
    implementation("com.github.darkryh.MoonGetter:moongetter-client-ktor:$version")   // Ktor (for multiplatform or coroutine preference)

    // Optional: Only for Ktor, cookie manager
    implementation("com.github.darkryh.MoonGetter:moongetter-client-cookie-java-net:$version")

    // Optional: Full server support
    implementation("com.github.darkryh.MoonGetter:moongetter-servers:$version")

    // Optional: Robot support (Android only for now)
    implementation("com.github.darkryh.MoonGetter:moongetter-core-robot:$version")
    implementation("com.github.darkryh.MoonGetter:moongetter-servers-robot:$version")
    implementation("com.github.darkryh.MoonGetter:moongetter-android-robot:$version")

    // Optional: Individual servers
    implementation("com.github.darkryh.MoonGetter:moongetter-mp4upload:$version")
    implementation("com.github.darkryh.MoonGetter:moongetter-filemoon:$version")
    implementation("com.github.darkryh.MoonGetter:moongetter-streamtape:$version")
    implementation("com.github.darkryh.MoonGetter:moongetter-streamwish:$version")
}
```

---

## 🧐 Client & Cookie Management Integration

| Client Library     | Module                                       | Cookie Management                 | KMP Support        |
|-------------------|----------------------------------------------|-----------------------------------|--------------------|
| **OkHttp**         | `moongetter-client-okhttp`                   | ✅ Built-in                        | ⏳ Not Supported    |
| **Ktor**           | `moongetter-client-ktor`                     | ⚠️ Manual setup required           | ✅ Experimental     |
| **JavaNetCookieManager** | `moongetter-client-cookie-java-net`    | ✅ (Only for Ktor, optional)       | ⏳ Planned         |

> ✅ = Supported / ⚠️ = Manual setup / ⏳ = Coming soon

**Note**:
- **OkHttp** has **built-in cookie support**, no setup needed.
- **Ktor** requires you to explicitly inject cookie management (e.g., `JavaNetCookieManagement`).

Example with Ktor:
```kotlin
MoonGetter.Builder()
    .setClient(
        KtorMoonClient(
            engineFactory = CIO,
            cookieManagement = JavaNetCookieManagement()
        )
    )
```

Example with OkHttp:
```kotlin
MoonGetter.Builder()
    .setClient(
        OkHttpClient()
    )
```

---

## 🚀 Example Usage

### ViewModel with coroutine logic:
```kotlin
class MyViewModel : ViewModel() {

    private val engine = Engine.Builder()
        .onCore(
            engines = arrayOf(
                Mp4UploadFactory,
                FilemoonFactory,
                StreamtapeFactory,
                StreamwishFactory,
                // CustomServerFactory (recommended to use `object`)
            )
        )
        .onRobot(
            AndroidRobot
                .Builder()
                .onContext(/* appContext */)
                .build()
        )
        .build()

    fun getMediaStreams(url: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val server = MoonGetter
                .Builder()
                .setClient(
                    KtorMoonClient(
                        engineFactory = CIO,
                        cookieManagement = JavaNetCookieManagement()
                    )
                )
                .setTimeout(8000)
                .setEngine(engine)
                .setHeaders(mapOf("User-Agent" to "Mozilla/5.0"))
                .get(url)

            val streams = server?.videos
        } catch (e: InvalidServerException) {
            Log.e("MoonGetter", "Invalid: ${e.message}", e)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
```

---

## 🛠 Add Your Own Custom Server

### Custom Server Class
```kotlin
class CustomServer(
    url: String,
    client: MoonClient,
    headers: HashMap<String, String>,
    configData: Configuration.Data
) : Server(url, client, headers, configData) {

    private val urlRegex =  """https://custom\.domain\.com/aqua/sv\?url=([^&]+)""".toRegex()

    override var url: String = urlRegex.find(url)?.groupValues?.get(1)
        ?: throw InvalidServerException(
            Resources.invalidProcessInExpectedUrlEntry(name),
            Error.INVALID_PROCESS_IN_EXPECTED_URL_ENTRY
        )

    override suspend fun onExtract(): List<Video> {
        val response = client.GET()

        if (!response.isSuccess) throw InvalidServerException(
            Resources.unsuccessfulResponse(name),
            Error.UNSUCCESSFUL_RESPONSE,
            response.statusCode
        )

        return listOf(
            Video(
                quality = DEFAULT,
                url = PatternManager.singleMatch(
                    string = response.body.asString(),
                    regex = "<source src=\"(.*?)\""
                ) ?: throw InvalidServerException(
                    Resources.expectedResponseNotFound(name),
                    Error.EXPECTED_RESPONSE_NOT_FOUND
                )
            )
        )
    }
}
```

### Custom Server Factory
```kotlin
object CustomServerFactory : Server.Factory {
    override val belongedClass = CustomServer::class.java
    override val pattern = "your regex here"
}
```

---

## 🧪 Requests

- `get(url: String)`: Retrieves a `Server?` for the given URL.
- `get(urls: List<String>)`: Retrieves a `List<Server>` for multiple URLs.
- `getUntilFindResource(urls: List<String>)`: Stops at the first valid `Server` found.

---

## 🤝 Want to Collaborate?
If you'd like to contribute or propose a new feature, feel free to reach out via [X / Twitter](https://x.com/Darkryh) @Darkryh or open a PR/request.

---

MoonGetter is in active development to expand KMP support. Swift/iOS support is **coming soon**.
