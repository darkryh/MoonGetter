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
```
# Kotlin Multiplatform Project Dependencies (MoonGetter Modules Only)

This section outlines the `dependencies` block specifically for integrating **MoonGetter modules** within a **Kotlin Multiplatform (KMP)** project structure. It focuses solely on MoonGetter-related dependencies, showing their placement for common shared code, Android-specific configurations, and iOS (Darwin) targets.

```kotlin
# Kotlin Multiplatform Project Dependencies (MoonGetter Modules Only)

This section outlines the `dependencies` block specifically for integrating **MoonGetter modules** within a **Kotlin Multiplatform (KMP)** project structure. It focuses solely on MoonGetter-related dependencies, showing their placement for common shared code, Android-specific configurations, iOS (Darwin) targets, and Desktop (JVM) targets.
```
```kotlin
dependencies {
    // --- Common Main (Shared KMP Logic for MoonGetter) ---
    // These MoonGetter modules are compiled for all targets (Android, iOS, Desktop JVM, etc.)
    // and contain the core shared logic of the library.
    commonMain.dependencies {
        // Required core logic for MoonGetter
        implementation("io.github.darkryh.moongetter:moongetter-core:2.0.0-alpha01")

        // MoonGetter Ktor Client - Recommended for Multiplatform compatibility.
        // This module provides the abstract Ktor client interface that MoonGetter uses.
        implementation("io.github.darkryh.moongetter:moongetter-client-ktor:2.0.0-alpha01")

        // Optional: Full server support bundle (includes comprehensive server implementations)
        implementation("io.github.darkryh.moongetter:moongetter-server-bundle:2.0.0-alpha01")

        // Optional: Core Robot support (if your robot logic has common components for all targets)
        implementation("io.github.darkryh.moongetter:moongetter-core-robot:2.0.0-alpha01")
        // Optional: Robot server bundle (if your robot server implementations are common across targets)
        implementation("io.github.darkryh.moongetter:moongetter-server-bundle-robot:2.0.0-alpha01")

        // Optional: Individual server modules (use these instead of `moongetter-server` for selective support)
        // implementation("io.github.darkryh.moongetter:moongetter-mp4upload:2.0.0-alpha01")
        // implementation("io.github.darkryh.moongetter:moongetter-filemoon:2.0.0-alpha01")
        // implementation("io.github.darkryh.moongetter:moongetter-streamtape:2.0.0-alpha01")
        // implementation("io.github.darkryh.moongetter:moongetter-streamwish:2.0.0-alpha01")
        // Add other individual server modules as needed
    }

    // --- Android Specific MoonGetter Dependencies ---
    // These dependencies are only applied when building for the Android platform.
    androidMain.dependencies {
        // Choose ONE of the following HTTP clients for Android:

        // Option 1: MoonGetter OkHttp Client (recommended for Android due to its robustness)
        implementation("io.github.darkryh.moongetter:moongetter-client-okhttp:2.0.0-alpha01")
        // Note: OkHttp has built-in cookie handling.
        // If you need specific JavaNet cookie/trust management with OkHttp, you can still add them:
        // implementation("io.github.darkryh.moongetter:moongetter-client-cookie-java-net:2.0.0-alpha01")
        // implementation("io.github.darkryh.moongetter:moongetter-client-trustmanager-java-net:2.0.0-alpha01")

        // OR

        // Option 2: Ktor Client with OkHttp engine (if you prefer Ktor's API but want OkHttp's backing)
        // You would typically use the `moongetter-client-ktor` from `commonMain` and
        // explicitly add the Ktor OkHttp engine here for Android:
        // implementation("io.ktor:ktor-client-okhttp:2.3.12") // Use the actual Ktor version from your libs.versions.toml
        // If using Ktor client, and you need explicit JavaNet-based cookie/trust management:
        // implementation("io.github.darkryh.moongetter:moongetter-client-cookie-java-net:2.0.0-alpha01")
        // implementation("io.github.darkryh.moongetter:moongetter-client-trustmanager-java-net:2.0.0-alpha01")


        // Android-specific Robot API implementations (e.g., for WebView-based extraction on Android)
        implementation("io.github.darkryh.moongetter:moongetter-android-robot:2.0.0-alpha01")
    }

    // --- iOS Specific MoonGetter Dependencies (Kotlin/Native - Darwin) ---
    // These dependencies are only applied when building for iOS targets (iosX64, iosArm64, iosSimulatorArm64).
    iosMain.dependencies {
        // REQUIRED: Ktor's Darwin engine is essential for the `moongetter-client-ktor` (from commonMain)
        // to perform actual network requests on iOS. It provides the concrete implementation
        // that connects to Apple's native networking APIs (NSURLSession).
        implementation("io.ktor:ktor-client-darwin:2.3.12") // Use the actual Ktor version from your libs.versions.toml
        // Note: Ktor's Darwin engine generally handles cookies via Foundation's URLSession,
        // so separate MoonGetter cookie/trust managers are usually not explicitly added here.
    }

    // --- Desktop (JVM) Specific MoonGetter Dependencies ---
    // These dependencies are only applied when building for desktop JVM applications.
    desktopMain.dependencies {
        // Choose ONE of the following HTTP clients for Desktop JVM:

        // Option 1: Ktor Client with CIO engine (recommended for KMP desktop targets for consistency with Ktor common API)
        // The `moongetter-client-ktor` from `commonMain` will use Ktor's CIO engine for Desktop JVM.
        implementation("io.ktor:ktor-client-cio:2.3.12") // Use the actual Ktor version from your libs.versions.toml
        // If using Ktor client, and you need explicit JavaNet-based cookie/trust management:
        implementation("io.github.darkryh.moongetter:moongetter-client-cookie-java-net:2.0.0-alpha01")
        implementation("io.github.darkryh.moongetter:moongetter-client-trustmanager-java-net:2.0.0-alpha01")

        // OR

        // Option 2: MoonGetter OkHttp Client (if you prefer OkHttp directly for desktop JVM)
        // implementation("io.github.darkryh.moongetter:moongetter-client-okhttp:2.0.0-alpha01")
        // Note: OkHttp has built-in cookie handling.
        // If you need specific JavaNet cookie/trust management with OkHttp, you can still add them:
        // implementation("io.github.darkryh.moongetter:moongetter-client-cookie-java-net:2.0.0-alpha01")
        // implementation("io.github.darkryh.moongetter:moongetter-client-trustmanager-java-net:2.0.0-alpha01")
    }
}
```

# Java/Kotlin JVM Project Dependencies (MoonGetter Integration Example)

This section outlines the `dependencies` block for integrating **MoonGetter modules** into a standard **Java or Kotlin JVM project**. This includes typical Android applications or standalone desktop JVM applications that **do not use Kotlin Multiplatform**.

```kotlin
dependencies {
    // --- Core MoonGetter Library ---
    // The main logic for stream extraction.
    implementation("io.github.darkryh.moongetter:moongetter-core:2.0.0-alpha01")

    // --- HTTP Client for MoonGetter ---
    // You have flexibility in choosing your HTTP client for JVM projects:

    // Option 1 (Recommended for simplicity): MoonGetter's dedicated OkHttp client.
    // OkHttp is a robust and widely used HTTP client for JVM, including Android.
    // It has built-in cookie management.
    implementation("io.github.darkryh.moongetter:moongetter-client-okhttp:2.0.0-alpha01")

    // Option 2: MoonGetter's Ktor client with a specific Ktor engine.
    // Use this if you prefer Ktor's API style. You will also need to add a Ktor engine.
    // implementation("io.github.darkryh.moongetter:moongetter-client-ktor:2.0.0-alpha01")
    // Choose one Ktor engine:
    // For general JVM/Android:
    // implementation("io.ktor:ktor-client-okhttp:2.3.12") // Use OkHttp as Ktor's backing
    // OR
    // implementation("io.ktor:ktor-client-cio:2.3.12") // Use Ktor's native CIO engine (often good for desktop JVM)

    // Optional: Cookie manager for Ktor client or explicit JavaNet handling with OkHttp.
    // If using Ktor, this is often needed for explicit cookie management.
    // If using OkHttp, this can be added for JavaNet-based cookie management if preferred.
    implementation("io.github.darkryh.moongetter:moongetter-client-cookie-java-net:2.0.0-alpha01")

    // Optional: Trust Manager for JavaNet (for custom SSL certificate trust, if needed)
    implementation("io.github.darkryh.moongetter:moongetter-client-trustmanager-java-net:2.0.0-alpha01")

    // --- Server Support ---
    // Optional: Full server support bundle (includes all common streaming server implementations)
    implementation("io.github.darkryh.moongetter:moongetter-server-bundle:2.0.0-alpha01")

    // Optional: Robot support for Android/JVM
    implementation("io.github.darkryh.moongetter:moongetter-core-robot:2.0.0-alpha01")
    implementation("io.github.darkryh.moongetter:moongetter-server-bundle-robot:2.0.0-alpha01")
    // This is specifically for Android-related robot API implementations.
    implementation("io.github.darkryh.moongetter:moongetter-android-robot:2.0.0-alpha01")

    // Optional: Individual server modules (use these instead of `moongetter-server` for selective support)
    // implementation("io.github.darkryh.moongetter:moongetter-mp4upload:2.0.0-alpha01")
    // implementation("io.github.darkryh.moongetter:moongetter-filemoon:2.0.0-alpha01")
    // implementation("io.github.darkryh.moongetter:moongetter-streamtape:2.0.0-alpha01")
    // implementation("io.github.darkryh.moongetter:moongetter-streamwish:2.0.0-alpha01")
    // Add any other individual server dependencies as needed
}
```

---

## 🧐 Client & Cookie Management Integration

| Client Library     | Module                                       | Cookie Management                 | KMP Support        |
|-------------------|----------------------------------------------|-----------------------------------|--------------------|
| **OkHttp**         | `moongetter-client-okhttp`                   | ✅ Built-in                        | ⏳ Not Supported    |
| **Ktor**           | `moongetter-client-ktor`                     | ⚠️ Manual setup required           | ✅ Experimental     |
| **JavaNetCookieManager** | `moongetter-client-cookie-java-net`    | ✅ (Only for Ktor, optional)       | ⏳ Not Supported    |

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
                        engineFactory = CIO, //on iOS Darwin
                        cookieManagement = JavaNetCookieManagement(), //on iOS MoonCookie.Management.newEmptyFactory()
                        trustManager = JavaMoonTrustManager // on iOS MoonTrust.Manager.newEmptyFactory()
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
