[![Sonatype Central](https://img.shields.io/maven-central/v/io.github.darkryh.moongetter/moongetter-core?label=Sonatype%20Central)](https://central.sonatype.com/artifact/io.github.darkryh.moongetter/moongetter-core/2.0.0-alpha01)
# MoonGetter for Kotlin

[![App Icon](assets/images/moon-getter.png)](https://play.google.com/store/apps/details?id=com.ead.project.moongetter)
[![Play Store Icon](assets/images/play-store.png)](https://play.google.com/store/apps/details?id=com.ead.project.moongetter)

**MoonGetter** is a robust, Kotlin-first library engineered for seamless stream extraction and video hosting download logic. It offers comprehensive support for Android, is actively expanding into **Kotlin Multiplatform (KMP)**, and features flexible custom server integration.

---

## ✅ Features

-   **Custom Server Integration** — Easily build and integrate your own streaming providers.
-   **Coroutine-based Architecture** — Async-first design for highly scalable extraction logic.
-   **Transparent HTTP Client Injection** — Works seamlessly with both OkHttp and Ktor.
-   **Custom Error Types** — Intuitive error handling via `InvalidServerException` and descriptive `Error` enums.
-   **Flexible Cookie Management** — Automatic for OkHttp, configurable for Ktor.

---

## 🔄 Compatibility Table

| Feature                | Android | JVM    | Kotlin Multiplatform |
| :--------------------- | :-----: | :----: | :------------------: |
| Core Library           |   ✅    |   ✅   |          ✅          |
| Robot Servers API      |   ✅    |   ✅   |    ⏳ (Planned)      |
| Server Implementations |   ✅    |   ✅   |          ✅          |

> ✅ = Supported / ⏳ = In development / JVM refers to non-Android Kotlin projects.

---

## 🌐 Supported Servers (Core)

> ✅ Actively maintained / 💥 Deprecated

-   ✅ Google Drive
-   ✅ Mediafire
-   ✅ Streamtape
-   ✅ PixelDrain
-   ✅ Okru
-   ✅ StreamWish
-   💥 Voe *(Deprecated)*
-   ✅ Senvid
-   ✅ Vihide
-   ✅ Hexload
-   ✅ YourUpload
-   ✅ Facebook
-   ✅ XTwitter
-   ✅ LuluStream
-   ✅ Mp4Upload
-   ✅ Uqload
-   ✅ Mixdrop
-   ✅ Doodstream
-   ✅ Vidguard
-   ✅ Filemoon

---

## 🤖 Robot Servers *(Optional)*

> These require the `moongetter-core-robot` module and a platform-specific Robot API.

-   💥 Fireload *(Deprecated)*
-   ✅ 1CloudFile

**Coming Soon:** GoodStream, Gofile, Abyss

---

## ☕ Installation (Gradle)

```kotlin
// For stable releases, use Maven Central
repositories {
    mavenCentral()
}

// For snapshot versions or specific alpha releases, you might need JitPack
// repositories {
//    maven { url = uri("[https://jitpack.io](https://jitpack.io)") }
// }

dependencies {
    // Required core logic
    implementation("io.github.darkryh.moongetter:moongetter-core:2.0.0-alpha01")

    // Choose your HTTP client. Pick one based on your target platform and preference.
    implementation("io.github.darkryh.moongetter:moongetter-client-okhttp:2.0.0-alpha01") // Recommended for Android/JVM
    // OR
    implementation("io.github.darkryh.moongetter:moongetter-client-ktor:2.0.0-alpha01")   // Recommended for KMP (Android, iOS, JVM, JS, etc.)

    // Optional: Cookie manager for Ktor client
    // For JVM/Android:
    implementation("io.github.darkryh.moongetter:moongetter-client-cookie-java-net:2.0.0-alpha01")
    // For iOS (Kotlin/Native): (Add a specific dependency for iOS cookie management if available, or manage manually)
    // implementation("io.github.darkryh.moongetter:moongetter-client-cookie-ios:2.0.0-alpha01") // Example, replace with actual module if it exists

    // Optional: Full server support (includes all common servers)
    implementation("io.github.darkryh.moongetter:moongetter-server:2.0.0-alpha01")

    // Optional: Robot support (Android/JVM for now)
    implementation("io.github.darkryh.moongetter:moongetter-core-robot:2.0.0-alpha01")
    implementation("io.github.darkryh.moongetter:moongetter-server-robot:2.0.0-alpha01")
    implementation("io.github.darkryh.moongetter:moongetter-android-robot:2.0.0-alpha01") // For Android-specific robot implementations

    // Optional: Individual server (useful for smaller builds or specific needs)
    implementation("io.github.darkryh.moongetter:moongetter-mp4upload:2.0.0-alpha01")
    implementation("io.github.darkryh.moongetter:moongetter-filemoon:2.0.0-alpha01")
    implementation("io.github.darkryh.moongetter:moongetter-streamtape:2.0.0-alpha01")
    implementation("io.github.darkryh.moongetter:moongetter-streamwish:2.0.0-alpha01")
    // Add other individual servers as needed
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
        OkHttpClient
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
    override val serverName: String = "MyCustomServer" // Must match the serverName in CustomServer
    override val pattern: String = """https://custom\.domain\.com/aqua/sv\?url=([^&]+)""" // Your regex pattern for URLs this server handles

    /**
     * Creates a new [CustomServer] instance configured to handle the given URL.
     *
     * @param url The URL to be processed by the server.
     * @param headers HTTP headers to be used during requests.
     * @param configData Configuration data required for server setup.
     * @param client The HTTP client used to perform network operations.
     *
     * @return An instance of [CustomServer] ready to process the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = CustomServer(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
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
