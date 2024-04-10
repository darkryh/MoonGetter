package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager

class Streamtape(context: Context, url : String) : Server(context,url) {

    override suspend fun onExtract() {
        initializeBrowser()

        val sourceCode = getSourceCodeFrom(url) ?: "null"

        val getVideoMetadata  = PatternManager.singleMatch(
            string = sourceCode,
            regex = """document\.getElementById\('robotlink'\)\.innerHTML\s*=\s*'.*?(get_video\?[^']+)'""",
            groupIndex = 1
        ) ?:   throw InvalidServerException("Streamtape resource couldn't find it or resource down")

        this@Streamtape.url = "https://streamtape.com/$getVideoMetadata"
        addDefault()
    }

}