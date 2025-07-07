package com.ead.lib.moongetter.utils

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.models.Server.Companion.DEFAULT
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.utils.PlaylistUtils.Companion.VIDEO_HLS_REGEX
import okio.IOException

/**
 * Utility for fetching and parsing HLS (.m3u8) master playlists into a list of video stream variants.
 *
 * This class encapsulates the complete workflow:
 *  1. Constructing and executing an HTTP GET request to retrieve the playlist content.
 *  2. Applying customizable HTTP headers for both base and master requests.
 *  3. Parsing the raw playlist text using a detailed regular expression to identify each variant stream.
 *  4. Mapping each found resolution-URL pair to a {@link Video} data model, preserving associated headers.
 *
 * **Thread-safety:** Uses only local variables and immutable state; safe for concurrent use across threads.
 * **Error handling:** Caller must handle IOExceptions from network failures; invalid or missing content
 *                  will result in an empty list rather than a null pointer.
 *
 * @property client      OkHttpClient instance for performing synchronous HTTP calls.
 * @property headers     Default set of HTTP headers to send when no override is provided.
 */
class PlaylistUtils(
    private val client: MoonClient,
    private val headers: Map<String, String> = emptyMap()
) {

    /**
     * Fetch and parse the HLS master playlist at the given URL.
     *
     * This method performs a synchronous HTTP GET to retrieve the playlist content, then parses
     * each line pair matching the HLS "#EXT-X-STREAM-INF" tag and its subsequent URL line.
     *
     * @param playlistUrl      Absolute URL of the master .m3u8 playlist to retrieve.
     * @param headers          Optional override of default HTTP headers; if null, default headers are used.
     * @param masterHeaders    Lambda to transform the base headers into the final set for the request.
     *                         This allows callers to inject authentication tokens or other custom values.
     *                         The default implementation will add or override the "Accept" header to.
     * @return                 A non-null list of Video models, each representing one quality-level stream.
     *                         If the network call fails or no variants are found, returns an empty list.
     * @throws IOException     Propagates IOExceptions from OkHttp for network failures or invalid responses.
     * @see extractFromHlsProvider
     */
    @Throws(IOException::class)
    suspend fun extractFromHls(
        playlistUrl: String,
        headers: Map<String, String>? = null,
        masterHeaders: (Map<String, String>) -> Map<String, String> = { base -> generateMasterHeaders(base) }
    ): List<Video> {
        return extractFromHlsProvider(playlistUrl, headers, masterHeaders)
    }

    /**
     * Internal implementation detail for playlist retrieval and parsing.
     *
     * This method is identical to {@link extractFromHls} but exposed privately
     * to factor out the core logic. It:
     *  - Builds the final headers by invoking [masterHeaders] on the provided or default headers.
     *  - Executes a blocking OkHttp call to fetch the playlist as a UTF-8 string.
     *  - Uses [VIDEO_HLS_REGEX] to find all quality-URL pairs.
     *  - Constructs a [Video] object for each match, copying the resolution, URL, and headers.
     *
     * @param playlistUrl      The master playlist URL.
     * @param headers          Optional headers override for a single extraction call.
     * @param masterHeaders    Header-transform function for final request headers.
     * @return                 List of Video entries extracted from the playlist.
     */
    private suspend fun extractFromHlsProvider(
        playlistUrl: String,
        headers: Map<String, String>? = null,
        masterHeaders: (Map<String, String>) -> Map<String, String> = { base -> generateMasterHeaders(base) }
    ): List<Video> {
        // 1) Determine headers to send: use override if provided, else default
        val requestHeaders = masterHeaders(headers ?: this.headers)

        return listOf(
            Video(
                quality = DEFAULT,
                url = playlistUrl,
                headers = requestHeaders
            )
        )

        // 2) Perform HTTP GET and read playlist body as a String
        /*val playlistText = client
            .request(GET(playlistUrl, requestHeaders.toHashMap()))
            .bodyAsText()

        // 3) Find all matches of the HLS variant pattern and map to Video
        return VIDEO_HLS_REGEX
            .findAll(playlistText)
            .map { match ->
                // Extracted values: resolution (e.g., "1920x1080") and stream URL
                val resolution = match.destructured.component1()
                val url        = match.destructured.component2()

                // Construct and return Video model
                Video(
                    quality = resolution,
                    url = url,
                    headers = requestHeaders.toHashMap()
                )
            }
            .toList()
            .ifEmpty {
                listOf(
                    Video(
                        quality = DEFAULT,
                        url = playlistUrl,
                        headers = requestHeaders.toHashMap()
                    )
                )
            }*/
    }

    /**
     * Helper to generate default headers for a master playlist request.
     * Ensures the Accept header allows any content type.
     *
     * @param baseHeaders   The initial set of headers to modify.
     * @return              A new Headers instance with "Accept: 8" set.
    */
    private fun generateMasterHeaders(baseHeaders: Map<String, String>): Map<String, String> =
        HashMap(
            baseHeaders
                .plus("Accept" to "*/*")
        )

    companion object {
        /**
         * Regex pattern to match HLS master playlist stream entries.
         *
         * Pattern breakdown:
         *  - `#EXT-X-STREAM-INF:` literal tag indicating a variant stream.
         *  - `.*?RESOLUTION=(\d+x\d+)` non-greedy up to "RESOLUTION={width}x{height}";
         *    capture group 1 = resolution string.
         *  - `.*?\n` non-greedy up to the end of the line.
         *  - `(https?://\S+)` capture group 2 = absolute URL starting with http:// or https://.
         *
         * Flags:
         *  - IGNORE_CASE: case-insensitive matching.
         */
        private val VIDEO_HLS_REGEX =
            """#EXT-X-STREAM-INF:.*?RESOLUTION=(\d+x\d+).*?\n(https?://\S+)"""
                .toRegex(option = RegexOption.IGNORE_CASE)
    }
}