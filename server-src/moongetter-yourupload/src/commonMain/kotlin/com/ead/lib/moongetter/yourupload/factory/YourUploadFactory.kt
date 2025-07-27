package com.ead.lib.moongetter.yourupload.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.yourupload.YourUpload
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration

/**
 * Factory responsible for creating instances of the [YourUpload] server.
 *
 * This factory is used to detect and handle video URLs hosted on the YourUpload platform.
 * It identifies URLs based on a regex pattern and initializes the appropriate server extractor.
 */
object YourUploadFactory : Server.Factory {

    /**
     * Unique name used to identify this server within the system.
     */
    override val serverName: String = "YourUpload"

    /**
     * Regular expression pattern used to match URLs from the YourUpload video hosting service.
     *
     * Example of a valid match:
     * - https://yourupload.com/embed/abc123xyz
     */
    override val pattern: String = """https?://.*yourupload\.com/embed/.*"""

    /**
     * Creates an instance of [YourUpload] for the given URL.
     *
     * @param url The full video URL to be processed.
     * @param headers Optional HTTP headers to include (e.g., User-Agent, Referer).
     * @param configData Optional custom configuration data required by the server.
     * @param client The internal HTTP client used to perform network operations.
     * @return A [YourUpload] instance capable of handling extraction logic.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = YourUpload(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}