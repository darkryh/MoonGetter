package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.core.system.extensions.delete
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import okhttp3.Request

class Bayfiles(context: Context, url : String) : Server(context,url) {

    override val isDeprecated: Boolean = true

    override suspend fun onExtract() : List<Video> {
        val response = OkHttpClient()
            .newCall(Request.Builder().url(url).build())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,Properties.BayfilesIdentifier))

        val countMetaData: List<String> = PatternManager.findMultipleMatches(
            string =  response.body?.string().toString(),
            regex = "https?:\\/\\/(cdn-[0123456789][0123456789][0123456789]).(bayfiles\\.com\\/.+)",
            groupIndex = 0
        ).ifEmpty { throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it,Properties.BayfilesIdentifier)) }

        return if (countMetaData.size > 1) {
            (0 ..countMetaData.size / 2).map {
                Video(
                    quality = quality(it),
                    url = fixDownloadLinks(countMetaData[it])
                )
            }
        }
        else {
            listOf(
                Video(
                    quality = DEFAULT,
                    url = fixDownloadLinks(countMetaData[0]))
            )
        }
    }

    private fun fixDownloadLinks(string: String): String {
        return string.trim { it <= ' ' }
            .delete("\"")
            .delete(" ")
            .delete("img")
            .delete(">")
            .delete("<")
    }

    private fun quality(index : Int) :String {
        when (index) {
            0 -> return "720p"
            1 -> return "480p"
        }
        return "Default"
    }
}