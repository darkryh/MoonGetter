package com.ead.lib.moongetter.models

import okhttp3.Headers
import java.net.URI

@Suppress("unused_parameter")
data class VideoPlaylist(val url: String,
                         val quality: String,
                         var videoUrl: String?,
                         val headers: Headers? = null,
                         val subtitleTracks: List<Track> = emptyList(),
                         val audioTracks: List<Track> = emptyList()
) {
    constructor(url: String,
                quality: String,
                videoUrl: String?,
                uri: URI? = null,
                headers: Headers? = null) : this(url, quality, videoUrl, headers)
}
