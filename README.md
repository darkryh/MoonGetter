[![](https://jitpack.io/v/darkryh/MoonGetter.svg)](https://jitpack.io/#darkryh/MoonGetter)
# MoonGetter for Kotlin

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
10. **Fembed**
11. **Vihide**
12. **Filemoon**
13. **Vidguard**
14. **GoodStream** in todo
15. **Hexload**
16. **1CloudFile**
17. **YourUpload**
18. **Facebook**
19. **XTwitter**
20. **Gofile** in todo
21. **LuluStream** in todo

## Installation - Gradle
```groovy  
repositories {   
    maven { url 'https://jitpack.io' }  
}

dependencies {  
    implementation("com.github.darkryh:moongetter-core:$version")
    
    // implement you wanted extractor server
    implementation("com.github.darkryh:moongetter-filemoon:$version")
    implementation("com.github.darkryh:moongetter-streamtape:$version")
    implementation("com.github.darkryh:moongetter-streamwish:$version")
} 
```  
# Example of Use Case
```kotlin
class MyViewModel : ViewModel() {

    private val engine : Engine = Engine.Builder()
        .onCore(
            engines = arrayOf(
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
                .initialize(context = event.context)
                .setHeaders(
                    mapOf(
                        "User-Agent" to "Mozilla/5.0"
                    )
                )
                .setTimeout(8000)
                .setEngine(engine)
                .get(event.url)
            
            val streamsResult = server.videos

        }
        catch (e : InvalidServerException) { 
            e.printStackTrace()
        }
        catch (e : IOException) {
            e.printStackTrace()
        }
        catch (e : RuntimeException) {
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
    context : Context,
    url :String,
    headers : HashMap<String,String>,
    configData: Configuration.Data
) : Server(context,url,headers,configData) {

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
                .initialize(context = event.context)
                .setEngine(engine)
                .get(event.url)

            val streamsResult = server.videos

        }
        catch (e : InvalidServerException) {
            e.printStackTrace()
        }
        catch (e : IOException) {
            e.printStackTrace()
        }
        catch (e : RuntimeException) {
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
f you want to help or collaborate, feel free to contact me on X account @Darkryh or just make a request.
