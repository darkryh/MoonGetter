[![](https://jitpack.io/v/darkryh/MoonGetter.svg)](https://jitpack.io/#darkryh/MoonGetter)

# MoonGetter for Kotlin

[![App Icon](assets/images/moon-getter.png)](https://play.google.com/store/apps/details?id=com.ead.project.moongetter)
[![Play Store Icon](assets/images/play-store.png)](https://play.google.com/store/apps/details?id=com.ead.project.moongetter)

**MoonGetter** is a powerful and versatile Kotlin library designed for stream extraction and file downloads. It empowers developers with the following features:

- **Custom server integration**: Easily add and configure your own extraction logic to suit unique requirements.
- **Kotlin coroutines**: Leverage asynchronous, non-blocking operations for improved performance and responsiveness.
- **Advanced error handling**: Streamline debugging with robust custom exceptions tailored to specific scenarios.
---

## Compatibility Table

| Feature                | Android | Kotlin | KMP |
|------------------------|:-------:|:------:|:---:|
| Core Library           |    ✅    |   ✅    |  ⏳  |
| Robot Servers API      |    ✅    |   ⏳    |  ⏳  |
| Server Implementations |    ✅    |   ✅    |  ⏳  |

> **Legend**:  
> ✅ - Supported  
> ⏳ - In progress

---

## Supported Servers

1. **Google Drive**
2. **Mediafire**
3. **Streamtape**
4. **PixelDrain**
5. **Okru**
6. **StreamWish**
7. **Voe**
8. **Senvid**
9. **Vihide**
10. **Filemoon**
11. **Hexload**
12. **YourUpload**
13. **Facebook**
14. **XTwitter**
15. **LuluStream**
16. **Mp4Upload**
17. **Uqload**
18. **Mixdrop**
19. **Doodstream**

---

## Robot Servers

> **Note**: These servers require the `moongetter-core-robot` dependency and the respective platform Robot API. For Android, use the `moongetter-android-robot` library. General Kotlin support is under development.

1. **Fireload**
2. **Vidguard**
3. **1CloudFile**

### TO-DO Servers

1. **GoodStream**
2. **Gofile**
3. **Abyss**

---

## Installation (Gradle)

Add JitPack to your repositories:

```groovy

repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // required
    implementation("com.github.darkryh.MoonGetter:moongetter-core:$version")
    // required only if you are using robot servers
    implementation("com.github.darkryh.MoonGetter:moongetter-core-robot:$version")
    // contains the whole servers, aside from robot servers
    implementation("com.github.darkryh.MoonGetter:moongetter-servers:$version")
    // contains the whole robot servers
    implementation("com.github.darkryh.MoonGetter:moongetter-servers-robot:$version")
    
    // required only if you are using robot servers and using for Android Platform
    implementation("com.github.darkryh.MoonGetter:moongetter-android-robot:$version")

    // Specific server implementations in case of need some of them
    implementation("com.github.darkryh.MoonGetter:moongetter-mp4upload:$version")
    implementation("com.github.darkryh.MoonGetter:moongetter-filemoon:$version")
    implementation("com.github.darkryh.MoonGetter:moongetter-streamtape:$version")
    implementation("com.github.darkryh.MoonGetter:moongetter-streamwish:$version")
}
```  
# Example of Use Case
```kotlin
class MyViewModel : ViewModel() {

    private val engine : Engine = Engine.Builder()
        .onCore(
            engines = arrayOf(
                Mp4UploadFactory,
                FilemoonFactory,
                StreamtapeFactory,
                StreamwishFactory,
                //YourCustomServerFactory recommended use object
            )
        )
        // Use this only if you want to use Robot-Servers
        .onRobot(
            AndroidRobot
                .Builder()
                .onContext(
                    context = /*context*/ passed in some way DI recommended
                )
                .build()
            // Note for the moment the library just provide an AndroidRobot Api
            // for android platform, still todo for Kotlin in General
        )
        .build()

    fun getMediaStreams(url: String) = viewModelScope.launch(Dispatchers.IO) {
        try {

            val server : Server? = MoonGetter
                .Builder()
                .setTimeout(8000)
                .setEngine(engine)
                .setHeaders(
                    mapOf(
                        "User-Agent" to "Mozilla/5.0"
                    )
                )
                .get(url)
            
            val streamsResult = server.videos

        }
        catch (e : InvalidServerException) { 
            e.printStackTrace()
        }
        catch (e : IllegalArgumentException) {
            e.printStackTrace()
        }
        catch (e : RuntimeException) {
            e.printStackTrace()
        }
        catch (e : IOException) {
            e.printStackTrace()
        }
    }
}
```

# Example to add Your Own Custom Server

### Creating your CustomServer Class
```kotlin
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video

class CustomServer(
    url :String,
    client : OkHttpClient,
    headers : HashMap<String,String>,
    configData: Configuration.Data
) : Server(url,client,headers,configData) {

    private val urlRegex =  "Regex example this is not really necesary".toRegex()

    override var url: String = urlRegex.find(url)?.groupValues?.get(1) ?:
    throw InvalidServerException(Resources.invalidProcessInExpectedUrlEntry(name), Error.INVALID_PROCESS_IN_EXPECTED_URL_ENTRY)

    override suspend fun onExtract(): List<Video> {
        val response = client
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.code)

        return listOf(
            Video(
                quality = DEFAULT,
                url = PatternManager.singleMatch(
                    string =  response.body?.string().toString(),
                    regex =  "<source src=\"(.*?)\""
                ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)
            )
        )
    }
}
```

### Creating your CustomServerFactory
You have to provide an object typed as Server.Factory
```kotlin
object CustomServerFactory : Server.Factory {
    override val belongedClass: Class<out Server> = CustomServer::class.java
    override val pattern: String = """your regex to identify"""
}
```

### Integrating your CustomServer into MoonGetter
You have to provide a an object typed as Server.Factory
```kotlin
class MyViewModel : ViewModel() {

    private val engine : Engine = Engine.Builder()
        .onCore(
            engines = arrayOf(
                CustomServerFactory,
                /**
                 * others custom or supported servers OkruFactory,FilemoonFactory, etc
                 */
            )
        )
        .build()

    fun getMediaStreams(url: String) = viewModelScope.launch(Dispatchers.IO) {
        try {

            val server : Server? = MoonGetter
                .Builder()
                .setEngine(engine)
                .get(url)

            val streamsResult = server.videos

        }
        catch (e : InvalidServerException) {
            e.printStackTrace()
        }
        catch (e : IllegalArgumentException) {
            e.printStackTrace()
        }
        catch (e : RuntimeException) {
            e.printStackTrace()
        }
        catch (e : IOException) {
            e.printStackTrace()
        }
    }
}
```
# Parameters

- **url**: URL to connect to and get a nullable server.
- **urls**: URLs to connect to and get a server list or a nullable server.

# Requests

- **get(url : String)**: Makes a request to get a nullable server from the `url` parameter.
- **getList(urls : List<String>)**: Makes a request to get a list of servers from the `urls` parameter.
- **getUntilFindResource()**: From the `urls` parameter, makes requests to find the first possible server and stops when one is found. If none is found, returns null.

# Want to collaborate
if you want to help or collaborate, feel free to contact me on X account @Darkryh or just make a request.
