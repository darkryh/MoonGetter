package com.ead.lib.moongetter.abyss

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import dev.whyoleg.cryptography.*
import dev.whyoleg.cryptography.algorithms.*
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64

class Abyss(url: String, client: MoonClient, headers: HashMap<String,String>, configData: Configuration.Data) : Server(url,client,headers,configData) {
    override suspend fun onExtract(): List<Video> {
        val slug = AbyssPayload.slug(url) ?: fail("Invalid Abyss embed URL")
        val page = client.GET()
        if (!page.isSuccess) fail("Abyss embed HTTP ${page.statusCode}", page.statusCode)
        val data = AbyssPayload.data(page.body.asString(), slug) ?: fail("No matching Abyss player metadata")
        val media = AbyssPayload.media(data)
        val sources = AbyssPayload.sources(media)
        if (sources.isEmpty()) fail("Abyss has no supported split MP4 source")
        val opened = mutableListOf<String>()
        try {
            return sources.take(3).map { source ->
                val playback = AbyssProxy.register(AbyssSession(source, page.url.toString, headers, configData.timeout))
                opened += playback.url
                Video(url = playback.url, quality = source.label, headers = emptyMap())
            }
        } catch (error: Throwable) {
            withContext(NonCancellable) { for (local in opened) AbyssProxy.release(local) }
            if (error is CancellationException) throw error
            if (error is Exception) fail("Abyss media could not be validated: ${error::class.simpleName}")
            throw error
        }
    }
    private fun fail(message: String, code: Int = 0): Nothing = throw InvalidServerException(message, Error.EXPECTED_RESPONSE_NOT_FOUND, code)
}

internal object AbyssPayload {
    const val PATTERN = """^https://(?:[A-Za-z0-9-]+\.)*(?:abyss\.to|argtesa\.top)/(?:\?(?:[^#\s]*&)?v=[A-Za-z0-9_-]{7,17}(?:&[^#\s]*)?|[A-Za-z0-9_-]{7,17}/?(?:\?[^#\s]*)?)$"""
    fun slug(value: String): String? {
        if (!Regex(PATTERN).matches(value)) return null
        val u = Url(value)
        return u.parameters["v"] ?: u.encodedPath.trim('/')
    }
    fun data(html: String, expected: String): JsonObject? {
        val payload = Regex("""\bconst\s+datas\s*=\s*["']([A-Za-z0-9+/=]+)["']""").find(html)?.groupValues?.get(1) ?: return null
        if (payload.length > 262144) return null
        // The browser uses JSON.parse(atob(...)), whose strings have Latin-1 byte semantics.
        val bytes = runCatching { Base64.Default.decode(payload) }.getOrNull() ?: return null
        val text = bytes.map { (it.toInt() and 255).toChar() }.joinToString("")
        val data = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
        return data.takeIf { (it["slug"] as? JsonPrimitive)?.content == expected }
    }
    suspend fun media(data: JsonObject): JsonObject {
        val value = data["media"] ?: error("Missing media")
        if (value is JsonObject) return value
        val secret = listOf("user_id", "slug", "md5_id").joinToString(":") { data[it]!!.jsonPrimitive.content }
        val binary = value.jsonPrimitive.content
        require(binary.length <= 262144 && binary.all { it.code <= 255 })
        return Json.parseToJsonElement(abyssDecrypt(secret, ByteArray(binary.length) { binary[it].code.toByte() }).decodeToString(throwOnInvalidSequence = true)).jsonObject
    }
    fun sources(media: JsonObject): List<AbyssSource> = (media["mp4"]?.jsonObject?.get("sources") as? JsonArray).orEmpty().mapNotNull { item ->
        runCatching {
            val s = item.jsonObject
            if (s["codec"]?.jsonPrimitive?.content != "h264") return@runCatching null
            val url = s["url"]!!.jsonPrimitive.content
            val path = s["path"]!!.jsonPrimitive.content
            val size = s["size"]!!.jsonPrimitive.long
            val part = s["partSize"]!!.jsonPrimitive.long
            val parsed = Url(url)
            require(parsed.protocol.name == "https" && parsed.host.endsWith(".sssrr.org") && parsed.port == 443 && parsed.user.isNullOrEmpty() && parsed.password.isNullOrEmpty() && parsed.encodedPath in setOf("", "/") && parsed.parameters.isEmpty())
            require(Regex("""[a-f0-9]/[a-f0-9]/[a-f0-9]/[a-f0-9]{29}\.[0-9]+\.[0-9]+""").matches(path))
            require(size > 0 && part in 65536..size && path.split('.')[1].toLong() == size)
            AbyssSource(url.trimEnd('/') + "/" + path, size, part, s["label"]!!.jsonPrimitive.content)
        }.getOrNull()
    }.distinctBy { it.label }
}
internal data class AbyssSource(val url: String, val size: Long, val partSize: Long, val label: String)

@OptIn(DelicateCryptographyApi::class)
internal suspend fun abyssDecrypt(secret: String, bytes: ByteArray): ByteArray {
    val provider = abyssCryptoProvider()
    val keyBytes = provider.get(MD5).hasher().hash(secret.encodeToByteArray()).joinToString("") { (it.toInt() and 255).toString(16).padStart(2,'0') }.encodeToByteArray()
    val key = provider.get(AES.CTR).keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, keyBytes)
    return key.cipher().decryptWithIv(keyBytes.copyOfRange(0,16), bytes)
}
