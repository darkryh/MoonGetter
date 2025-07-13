@file:Suppress("VisibleForTests")

package com.ead.lib.moongetter.vidguard

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.PlaylistUtils
import com.ead.lib.moongetter.utils.Values.targetUrl
import com.ead.lib.moongetter.vidguard.util.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeJSON
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class Vidguard(
    url: String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData: Configuration.Data
) : Server(url,client,headers,configData) {

    private val playlistUtils by lazy { PlaylistUtils(client, headers) }

    override val headers: HashMap<String, String> = headers.also {
        it["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        it["Accept-Language"] = "en-US,en;q=0.5"
        it["Priority"] = "u=0, i"
        it["Origin"] = url
        it["Referer"] = url
    }

    override var url: String = targetUrl ?: url

    override suspend fun onExtract(): List<Video> {
        val response = client
            .GET()

        if (!response.isSuccess) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.statusCode)

        val data = PatternManager.singleMatch(
            string =  response.body.asString().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) },
            regex = """<script\b[^>]*>((?:(?!<\/script>).)*?eval(?:(?!<\/script>).)*)<\/script>""",
            patternFlag = RegexOption.DOT_MATCHES_ALL
        ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

        val stream = PatternManager.singleMatch(
            string = executeRunnableScript(data),
            regex = "\"stream\"\\s*:\\s*\"([^\"]+)\""
        ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)


        val playListUrl = decodeObstructionData(stream)

        return playlistUtils.extractFromHls(playListUrl)
    }

    private suspend fun executeRunnableScript(scriptCode: String): String = withContext(Dispatchers.Vidguard) {
        val result: String
        val rhinoContext = Context.enter()

        try {

            rhinoContext.initSafeStandardObjects()
            rhinoContext.isInterpretedMode = true

            val executorScriptable: Scriptable = rhinoContext.initSafeStandardObjects()
            executorScriptable.put("window", executorScriptable, executorScriptable)

            rhinoContext.evaluateString(
                executorScriptable,
                scriptCode,
                "JavaScript",
                1,
                null
            )

            val svgObject = executorScriptable.get("svg", executorScriptable)

            result = if (svgObject is NativeObject) {
                NativeJSON.stringify(Context.getCurrentContext(), executorScriptable, svgObject, null, null).toString()
            } else {
                Context.toString(svgObject)
            }
        } finally {
            Context.exit()
        }

        result
    }

    private fun decodeObstructionData(url: String): String {
        val obstructedData = url.split("sig=")[1].split("&")[0]
        val finalData = obstructedData.chunked(2)
            .joinToString("") { (Integer.parseInt(it, 16) xor 2).toChar().toString() }
            .let {
                val padding = when (it.length % 4) { 2 -> "==" 3 -> "=" else -> "" }
                @OptIn(ExperimentalEncodingApi::class)
                String(Base64.decode((it + padding).toByteArray(Charsets.UTF_8)))
            }
            .dropLast(5)
            .reversed()
            .toCharArray()
            .apply {
                for (i in indices step 2) {
                    if (i + 1 < size) {
                        this[i] = this[i + 1].also { this[i + 1] = this[i] }
                    }
                }
            }
            .concatToString()
            .dropLast(5)

        return url.replace(obstructedData, finalData)
    }
}