package com.ead.lib.moongetter.mega
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.CancellationException

class Mega(url:String,client:MoonClient,headers:HashMap<String,String>,configData:Configuration.Data):Server(url,client,headers,configData) {
    override suspend fun onExtract():List<Video> = try {
        val session=MegaProxy.open(url,configData.timeout.toLong())
        listOf(Video(url=session.url))
    } catch(cancelled:CancellationException) { throw cancelled }
      catch(error:Exception) { throw InvalidServerException("MEGA local streaming could not start: ${error.message?.takeIf { !it.contains("://") } ?: "upstream connection failed"}",Error.EXPECTED_RESPONSE_NOT_FOUND) }
}
