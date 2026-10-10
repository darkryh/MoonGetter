@file:Suppress("RestrictedApi", "VisibleForTests")
package com.ead.lib.moongetter.gofile

import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.request.HttpMethod
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.vidguard.factory.util.executeRunnableScript
import io.ktor.http.Url
import kotlinx.serialization.json.*
import kotlinx.coroutines.CancellationException

/** Free website session flow. Never creates or requires a Premium subscription. */
class Gofile(url: String, client: MoonClient, headers: HashMap<String, String>, configData: Configuration.Data) :
    Server(url, client, headers, configData) {
    override suspend fun onExtract(): List<Video> {
        val code = Regex("""^https://(?:www\.)?gofile\.io/d/([A-Za-z0-9]+)(?:[/?#][^\s]*)?$""").find(url)?.groupValues?.get(1)
            ?: throw InvalidServerException("Invalid GoFile folder URL", Error.INVALID_BUILDER_PARAMETERS)
        val ua = headers.entries.firstOrNull { it.key.equals("User-Agent", true) }?.value ?: "Mozilla/5.0"
        val language = "en-US"
        val apiHeaders = headers.filterKeys { !it.equals("Authorization", true) && !it.equals("Cookie", true) && !it.equals("User-Agent", true) } +
            mapOf("User-Agent" to ua, "Origin" to "https://gofile.io", "Referer" to url, "X-BL" to language)
        // Each extraction obtains fresh signing code; retired website secrets are never cached.
        val scriptResponse = client.GET(requestUrl = "https://gofile.io/js/wt.obf.js", overrideHeaders = apiHeaders)
        if (!scriptResponse.isSuccess) fail("Website generator HTTP ${scriptResponse.statusCode}", scriptResponse.statusCode)
        val script = scriptResponse.body.asString()
        if (script.length !in 1..262144 || !script.contains("generateWT")) fail("Unsupported GoFile website generator")
        val guest = client.request<String>(Request(method = HttpMethod.POST, url = "https://api.gofile.io/accounts", headers = apiHeaders))
        val account = GofilePayload.data(guest.body.asString(), guest.statusCode, "guest account")
        val token = account["token"]?.jsonPrimitive?.contentOrNull?.takeIf { it.matches(Regex("[A-Za-z0-9]{32}")) }
            ?: fail("GoFile did not return a valid guest token")
        val wrapper = "var navigator={userAgent:" + JsonPrimitive(ua) + ",language:" + JsonPrimitive(language) + "};var globalThis=this;\n" + script +
            "\nvar svg={token:generateWT(" + JsonPrimitive(token) + ")};"
        val signed = try { Json.parseToJsonElement(executeRunnableScript(wrapper)).jsonObject["token"]?.jsonPrimitive?.contentOrNull }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { fail("GoFile website token generator is incompatible with this runtime") }
            ?.takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) } ?: fail("Unsupported GoFile website token")
        val listing = client.GET(requestUrl = "https://api.gofile.io/contents/$code?page=1&pageSize=100&sortField=name&sortDirection=1", overrideHeaders =
            apiHeaders + mapOf("Authorization" to "Bearer $token", "X-Website-Token" to signed))
        val data = GofilePayload.data(listing.body.asString(), listing.statusCode, "folder listing")
        val sources = GofilePayload.sources(data)
        if (sources.isEmpty()) fail("GoFile returned no accessible video files in the first folder page")
        // Only the guest session cookie goes to media hosts, never the API/signing headers.
        val mediaHeaders = mapOf("User-Agent" to ua, "Referer" to url, "Cookie" to "accountToken=$token")
        return sources.map { Video(url = it, headers = mediaHeaders) }
    }
    private fun fail(message: String, code: Int = 0): Nothing = throw InvalidServerException(message, Error.EXPECTED_RESPONSE_NOT_FOUND, code)
}

internal object GofilePayload {
    fun data(body: String, http: Int, stage: String = "request"): JsonObject {
        val envelope = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: throw InvalidServerException("GoFile $stage returned a non-JSON API response (HTTP $http)", Error.EXPECTED_RESPONSE_NOT_FOUND, http)
        val status = envelope["status"]?.jsonPrimitive?.contentOrNull
        if (status != "ok" || http !in 200..299) throw InvalidServerException(
            when(status) {
                "error-notPremium" -> "GoFile rejected this free website session (error-notPremium); no video source was received"
                "error-rateLimit" -> "GoFile rate limited the request; retry later"
                else -> "GoFile API failed: ${status ?: "unknown status"}"
            }, Error.UNSUCCESSFUL_RESPONSE, http)
        val data = envelope["data"] as? JsonObject ?: throw InvalidServerException("GoFile API data missing", Error.EXPECTED_RESPONSE_NOT_FOUND)
        if (data["canAccess"]?.jsonPrimitive?.booleanOrNull == false) throw InvalidServerException("GoFile folder requires access permission or a password", Error.EXPECTED_RESPONSE_NOT_FOUND)
        return data
    }
    fun sources(data: JsonObject): List<String> {
        val entries = if (data["type"]?.jsonPrimitive?.contentOrNull == "file") listOf(data)
            else (data["children"] as? JsonObject)?.values?.mapNotNull { it as? JsonObject }.orEmpty()
        return entries.filter { it["type"]?.jsonPrimitive?.contentOrNull == "file" }.mapNotNull { entry ->
            val mime = entry["mimetype"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val name = entry["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (!mime.startsWith("video/") && !name.substringAfterLast('.').lowercase().let { it in setOf("mp4", "mkv", "webm", "mov", "m4v") }) return@mapNotNull null
            val link = entry["link"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val parsed = runCatching { Url(link) }.getOrNull() ?: return@mapNotNull null
            link.takeIf { parsed.protocol.name == "https" && parsed.host.endsWith(".gofile.io") && !link.any(Char::isWhitespace) }
        }.distinct()
    }
}
