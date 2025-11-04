package com.ead.lib.moongetter.vidguard

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.extractFirst
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.ExtractionStrategy
import com.ead.lib.moongetter.utils.PlaylistUtils
import com.ead.lib.moongetter.utils.Values.targetUrl
import com.ead.lib.moongetter.utils.runWithStackSize
import com.ead.lib.moongetter.vidguard.factory.util.decodeObstructionData
import com.ead.lib.moongetter.vidguard.factory.util.executeRunnableScript

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
class Vidguard(
    url: String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData: Configuration.Data
) : Server(url, client, headers, configData) {
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
        // Use retry mechanism for resilient extraction
        val html = ExtractionStrategy.withRetry(
            config = ExtractionStrategy.RetryConfig.Default
        ) {
            val response = client.GET()
            
            if (!response.isSuccess) {
                throw InvalidServerException(
                    Resources.unsuccessfulResponse(name),
                    Error.UNSUCCESSFUL_RESPONSE,
                    response.statusCode
                )
            }
            
            response.body.asString().ifEmpty { 
                throw InvalidServerException(
                    Resources.emptyOrNullResponse(name), 
                    Error.EMPTY_OR_NULL_RESPONSE
                ) 
            }
        }

        // Extract script with eval using extension method
        val scriptData = runWithStackSize(1 * 1024 * 1024) {
            html.extractFirst(
                """<script\b[^>]*>((?:(?!<\/script>).)*?eval(?:(?!<\/script>).)*)<\/script>""",
                patternFlag = RegexOption.MULTILINE
            ) ?: throw InvalidServerException(
                Resources.expectedResponseNotFound(name), 
                Error.EXPECTED_RESPONSE_NOT_FOUND
            )
        }

        // Execute script and extract stream
        val executedScript = executeRunnableScript(scriptData)
        
        val stream = executedScript.extractFirst(
            """"stream"\s*:\s*"([^"]+)""""
        ) ?: throw InvalidServerException(
            Resources.expectedResponseNotFound(name), 
            Error.EXPECTED_RESPONSE_NOT_FOUND
        )

        // Decode obfuscated data
        val playlistUrl = decodeObstructionData(stream)

        return playlistUtils.extractFromHls(playlistUrl)
    }
}