[![](https://jitpack.io/v/darkryh/MoonGetter.svg)](https://jitpack.io/#darkryh/MoonGetter)
# MoonGetter for Kotlin

<p style="display: flex; justify-content: center; gap: 10px;">
  <img src="assets/images/moon-getter.png" alt="Bypass Cloudflare" width="256" />
  <img src="assets/images/playstore.png" alt="Play Store Icon" width="220" />
</p>

MoonGetter is an Android library for handling stream extraction and downloads. It offers the following features:
- Support for adding custom servers
- Utilizes the power of Kotlin coroutines
- Handles errors with custom exceptions

## Supported Servers

1. **Google Drive**
2. **Mediafire**
3. **Streamtape**
4. **Fireload**
5. **PixelDrain**
6. **Okru**
7. **StreamWish**
8. **Voe**
9. **Senvid**
10. **Vihide**
11. **Filemoon**
12. **Vidguard**
13. **Hexload**
14. **1CloudFile**
15. **YourUpload**
16. **Facebook**
17. **XTwitter**
18. **LuluStream**
19. **Mp4Upload**
20. **Uqload**
21. **Mixdrop**
22. **Doodstream**

### TO-DO Servers
1. **GoodStream**
2. **Gofile**
3. **Abyss**

## Installation - Gradle
```groovy  
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {  
    implementation("com.github.darkryh.MoonGetter:moongetter-core:$version")
    // the servers that use web-view, needs to use this core reference too
    // the following servers are Fireload,1CloudFile(OneCloudFile),Vidguard,GoFile(to-do)
    implementation("com.github.darkryh.MoonGetter:moongetter-core-robot:$version")
    
    //implementation containing the whole supported servers
    implementation("com.github.darkryh.MoonGetter:moongetter-servers:$version")
    //In case of need servers that user web-view
    implementation("com.github.darkryh.MoonGetter:moongetter-servers-robot:$version")
    
    // implement you wanted extractor server in case of need an specific server
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
        .build()

    fun getMediaStreams(context: Context, url: String) = viewModelScope.launch(Dispatchers.IO) {
        try {

            val server : Server? = MoonGetter
                .initialize(context = context)
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
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(context, url, client, headers, configData) {

    private val urlRegex = "Regex example this is not really necesary".toRegex()

    // Override the URL to connect to another URL if needed
    override var url: String = urlRegex.find(url)?.groupValues?.get(1)
        ?: throw InvalidServerException(context.getString(R.string.unknown_error))

    override suspend fun onExtract() : List<Video> {
        // Perform your extraction process

        // return a video list to handle in case of empty just return InvalidServerException
        return listOf(
            Video(
                quality = DEFAULT,
                url = "extracted url"
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

    fun getMediaStreams(context: Context, url: String) = viewModelScope.launch(Dispatchers.IO) {
        try {

            val server : Server? = MoonGetter
                .initialize(context = context)
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
