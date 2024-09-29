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
10. **Anonfiles**
11. **Bayfiles**
12. **Fembed**
13. **StreamSb**
14. **Filelions**

## Token Server

These servers require token authentication to work:

1. **1Fichier**

## Installation - Gradle
```groovy  
repositories {   
    maven { url 'https://jitpack.io' }  
}

dependencies {  
    implementation("com.github.darkryh:MoonGetter:$version")
} 
```  
# Example of Use Case
```kotlin
class MyViewModel : ViewModel() {

    fun getMediaStreams(context: Context, url: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            
            val server: Server = MoonGetter
                .initialize(context = context)
                .connect(url = url)
                .get()
            
            val streamsResult = server.videos

        }
        catch (e : InvalidServerException) { 
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

class CustomServer(context: Context, url: String) : Server(context, url) {

    private val urlRegex = "Regex example this is not really necesary".toRegex()

    // Override the URL to connect to another URL if needed
    override var url: String = urlRegex.find(url)?.groupValues?.get(1)
        ?: throw InvalidServerException(context.getString(R.string.unknown_error))

    override suspend fun onExtract() {
        // Perform your extraction process

        // Add the video to validate the server extraction
        add(Video())

        // You can call add() multiple times if the server has multiple videos
    }
}
```

### Integrating your CustomServer into MoonGetter
You have to provide a list of ServerIntegration type
```kotlin
ViewModel() {

	fun getMediaStreams(context : Context, url : String) = viewModelScope.launch(IO) {

		val server : Server = MoonGetter
                            .initialize(context = context)
                            .connect(url = url)
                            .setCustomServers(
                                listOf(
                                     ServerIntegration(
                                        serverClass = CustomServer::class.java,
                                        pattern = "Your regex pattern to identify when the server is called"
                                    )
                                )
                            )
                            .get()
				
		val streamsResult = server.videos
	}
}
```
# Parameters

- **url**: URL to connect to and get a nullable server.
- **urls**: URLs to connect to and get a server list or a nullable server.
- **set1FichierToken**: Token provider to enable 1Fichier Server.

# Requests

- **get()**: Makes a request to get a nullable server from the `url` parameter.
- **getList()**: Makes a request to get a list of servers from the `urls` parameter.
- **getUntilFindResource()**: From the `urls` parameter, makes requests to find the first possible server and stops when one is found. If none is found, returns null.

# In To-do
- **Filemoon**
- **Gofile**
- **Vidguard**

# Want to colaborate
f you want to help or collaborate, feel free to contact me on X account @Darkryh or just make a request.
