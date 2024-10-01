package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import okhttp3.Request

@Pending
@Unstable(reason = "Needs to validate direct link with header requests")
class Filemoon(context: Context,url: String) : Server(context,url) {

    override suspend fun onExtract() {
        val request: Request =  Request.Builder().url(url).build()

        val response = OkHttpClient()
            .newCall(request)
            .await()

        if(!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, Properties.FilemoonIdentifier))

        val dataContainer = PatternManager.singleMatch(
            string = response.body?.string().toString(),
            regex = """<script\s+data-cfasync='false'\s+type='text/javascript'>\s*eval\(function\(.*?\)\)\s*<\/script>""",
            groupIndex = 0
        ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, Properties.FilemoonIdentifier))

        /*val evalCode = PatternManager.singleMatch(
            string = dataContainer,
            regex = "<script[^>]*>([\\s\\S]*?)</script>\\s*",
            groupIndex = 1
        ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, Properties.FilemoonIdentifier))*/

        val dataGroup = PatternManager.singleMatch(
            string = dataContainer,
            regex = """(?<=\|function\|)(.*?)(?=\|file\|)""",
            groupIndex = 0
        )?.fixGroupData() ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, Properties.FilemoonIdentifier))

        val dataList = dataGroup.split("|")

        val downloadHost = dataList.last()
        val subDomain1 = dataList[dataList.size - 2]
        val subDomain2 = dataList[dataList.size - 3]
        val cdn = dataList[dataList.size - 4]
        val cdnEndingHost = dataList[dataList.size - 5]
        val hls = dataList[dataList.size - 6]
        val primaryId = dataList[dataList.size - 7]
        val secondaryId = dataList[dataList.size - 8]
        val fileId = dataList[dataList.size - 9]
        val master = dataList[dataList.size - 10]
        val fileExtension = dataList[dataList.size - 11]
        var t = dataList[dataList.size - 12]
        val tSecond = dataList[dataList.size - 13]

        val s = PatternManager.singleMatch(
            string = dataGroup,
            regex = "\\|data\\|(\\d+)\\|(\\d+)\\|video_ad\\|",
            groupIndex = 1
        )

        val e = PatternManager.singleMatch(
            string = dataGroup,
            regex = "\\|srv\\|(\\d+)",
            groupIndex = 1
        )

        t = if (tSecond != e) {
            "$t-$tSecond"
        } else {
            t
        }

        val f = PatternManager.singleMatch(
            string = dataGroup,
            regex = "\\|data\\|(\\d+)\\|(\\d+)\\|video_ad\\|",
            groupIndex = 2
        )

        val srvContainer = PatternManager.singleMatch(
            string = dataContainer,
            regex = "\\?([^\"]*t=(\\d+)[^\"]*s=([a-zA-Z\\d]+)[^\"]*)",
            groupIndex = 1
        ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, Properties.FilemoonIdentifier))

        val regex = Regex("\\b\\d{2}\\b")
        val matches = regex.findAll(srvContainer)
        val numbers = matches.map { it.value.toInt() }.toList()
        val srv = numbers.minOrNull() ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, Properties.FilemoonIdentifier))

        val asn = PatternManager.singleMatch(
            string = dataGroup,
            regex = "(\\d+)\\|asn\\|",
            groupIndex = 1
        )

        val sp = PatternManager.singleMatch(
            string = dataGroup,
            regex = "\\|(\\d+)\\|sp\\|",
            groupIndex = 1
        )

        url = "https://$downloadHost.$subDomain1.$subDomain2.$cdn.$cdnEndingHost/$hls/$primaryId/$secondaryId/$fileId/$master.$fileExtension?t=$t&s=$s&e=$e&f=$f&srv=$srv&asn=$asn&sp=$sp"

        addDefault()
    }
    private fun String.fixGroupData(): String {
        return this.replace("|||","|")
            .replace("||","|")
    }
}