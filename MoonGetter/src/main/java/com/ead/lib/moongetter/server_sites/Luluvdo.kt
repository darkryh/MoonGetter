package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.UnPacker
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

class Luluvdo(context: Context, url : String) : Server(context,url) {

    private val scriptPattern = Pattern.compile("<script type=[\"']text/javascript[\"']>(.*?)</script>", Pattern.DOTALL)

    override suspend fun onExtract() {
        val response = OkHttpClient()
            .newCall(Request.Builder().url(url).build())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(
            R.string.server_domain_is_down,
            Properties.LuluvdoIdentifier))

        url = PatternManager.singleMatch(
            string = response.body?.string().toString(),
            regex = "<script type=[\"']text/javascript[\"']>(.*?)</script>",
            patternFlag = Pattern.DOTALL
        ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, Properties.LuluvdoIdentifier))

        url = getDirectLink(url) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, Properties.LuluvdoIdentifier))

        addDefault()
    }

    private fun getDirectLink(scriptContent : String?): String? {
        return scriptContent?.let { script ->
            val unpackedScript = UnPacker.unpack(script)
            extractUrlFromScript(unpackedScript)
        }
    }

    private fun extractUrlFromScript(script: String?): String? {
        return script?.let {
            val urlPattern = Pattern.compile("""file:\s*"(.*?)"""", Pattern.DOTALL)
            val matcher = urlPattern.matcher(it)
            if (matcher.find()) matcher.group(1) else null
        }
    }
}