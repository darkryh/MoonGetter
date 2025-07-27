package com.ead.lib.moongetter.googledrive.factory

import com.ead.lib.moongetter.googledrive.GoogleDrive
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [GoogleDrive] server.
 *
 * This factory defines a regex pattern to detect Google Drive file URLs
 * and creates server instances capable of handling those URLs.
 */
object GoogleDriveFactory : Server.Factory {

    /**
     * Unique identifier for this server factory, useful for logging and management.
     */
    override val serverName: String = "GoogleDrive"

    /**
     * Regex pattern matching Google Drive URLs such as:
     * - https://drive.google.com/file/d/FILE_ID
     * - https://drive.google.com/open?id=FILE_ID
     * - https://drive.google.com/uc?id=FILE_ID
     */
    override val pattern: String = "https?:\\/\\/(www\\.)?drive\\.google\\.com\\/(?:file\\/d\\/|open\\?id=|uc\\?id=)([\\w-]+)"

    /**
     * Creates a new [GoogleDrive] server instance configured for the given URL.
     *
     * @param url The Google Drive URL to process.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data needed for server operation.
     * @param client HTTP client used for network requests.
     *
     * @return A fully configured [GoogleDrive] server instance.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = GoogleDrive(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}