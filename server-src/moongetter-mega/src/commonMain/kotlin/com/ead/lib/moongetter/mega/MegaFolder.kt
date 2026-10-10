package com.ead.lib.moongetter.mega

import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import kotlin.random.Random

internal data class MegaFolderLink(val handle: String, val secret: ByteArray, val folder: String?, val file: String?) {
    companion object {
        fun parse(url: String): MegaFolderLink {
            val match = Regex("""^https://mega\.nz/folder/([A-Za-z0-9_-]{8})#([A-Za-z0-9_-]{22})(?:/folder/([A-Za-z0-9_-]{8}))?(?:/file/([A-Za-z0-9_-]{8}))?$""")
                .matchEntire(url) ?: error("Expected a MEGA public folder link including its key")
            val key = MegaLink.decode(match.groupValues[2])
            require(key.size == 16)
            return MegaFolderLink(match.groupValues[1], key, match.groupValues[3].ifEmpty { null }, match.groupValues[4].ifEmpty { null })
        }
    }
}

internal object MegaFolder {
    private val videoExtensions = setOf("mp4", "m4v", "mov", "mkv", "webm", "avi", "wmv", "flv", "ts", "m2ts", "mpeg", "mpg", "ogv", "3gp")

    suspend fun resolve(url: String, http: HttpClient): MegaLink {
        if (!url.startsWith("https://mega.nz/folder/")) return MegaLink.parse(url)
        val folder = MegaFolderLink.parse(url)
        try {
            val response = http.post("https://g.api.mega.co.nz/cs") {
                parameter("id", Random.nextInt(1, Int.MAX_VALUE))
                parameter("n", folder.handle)
                contentType(ContentType.Application.Json)
                setBody("""[{"a":"f","c":1,"r":1}]""")
            }
            check(response.status.value == 200) { "MEGA folder API HTTP ${response.status.value}" }
            val element = Json.parseToJsonElement(response.bounded(4 * 1024 * 1024).decodeToString(throwOnInvalidSequence = true)).jsonArray.single()
            if (element is JsonPrimitive) error("MEGA folder API error ${element.content}")
            return select(folder, element.jsonObject["f"]!!.jsonArray)
        } finally { folder.secret.fill(0) }
    }

    /** Counts videos only after decrypting every candidate; unreadable nodes cannot silently hide ambiguity. */
    suspend fun select(folder: MegaFolderLink, nodes: JsonArray): MegaLink {
        require(nodes.size in 1..10_000) { "MEGA folder is empty or exceeds the node limit" }
        val entries = nodes.map { it.jsonObject }
        val byHandle = entries.associateBy { it["h"]!!.jsonPrimitive.content }
        require(byHandle.size == entries.size && byHandle.keys.all { Regex("[A-Za-z0-9_-]{8}").matches(it) })
        val root = entries.singleOrNull { it["t"]!!.jsonPrimitive.int == 1 && it["p"]?.jsonPrimitive?.content !in byHandle }
            ?: error("MEGA folder root is invalid")
        val rootHandle = root["h"]!!.jsonPrimitive.content
        val children = entries.groupBy { it["p"]?.jsonPrimitive?.content }
        fun descendants(start: String): Set<String> {
            val found = mutableSetOf<String>()
            val pending = ArrayDeque<String>()
            pending.add(start)
            while (pending.isNotEmpty()) {
                val handle = pending.removeFirst()
                check(found.add(handle)) { "MEGA folder tree contains a cycle" }
                children[handle].orEmpty().forEach { pending.add(it["h"]!!.jsonPrimitive.content) }
            }
            return found
        }
        val all = descendants(rootHandle)
        require(all.size == nodes.size) { "MEGA folder contains disconnected nodes" }
        val scope = folder.folder ?: rootHandle
        require(scope in all && byHandle[scope]!!["t"]!!.jsonPrimitive.int == 1) { "MEGA selected folder is unavailable" }
        val scoped = if (scope == rootHandle) all else descendants(scope)
        val candidates = if (folder.file != null) {
            require(folder.file in scoped) { "MEGA selected file is not in this folder" }
            listOf(byHandle[folder.file]!!)
        } else scoped.map { byHandle[it]!! }.filter { it["t"]!!.jsonPrimitive.int == 0 }
        val videos = mutableListOf<MegaLink>()
        var chosen: MegaLink? = null
        try {
            for (node in candidates) {
                require(node["t"]!!.jsonPrimitive.int == 0) { "MEGA selected node is not a file" }
                val key = unwrap(node["k"]!!.jsonPrimitive.content, rootHandle, folder.secret)
                val link = MegaLink(node["h"]!!.jsonPrimitive.content, key, folder.handle)
                try {
                    val name = MegaCipher(link).attributes(node["a"]!!.jsonPrimitive.content)
                    val size = node["s"]!!.jsonPrimitive.long
                    if (name.substringAfterLast('.', "").lowercase() in videoExtensions) {
                        require(size > 0) { "MEGA selected video is empty" }
                        videos.add(link.copy(expectedName = name, expectedSize = size))
                    } else { link.clear(); require(folder.file == null) { "MEGA selected file is not a video" } }
                } catch (error: Throwable) { link.clear(); throw error }
            }
            check(videos.isNotEmpty()) { "MEGA folder contains no video files" }
            check(videos.size == 1) { "MEGA folder contains ${videos.size} videos; select one with /file/<file-handle>" }
            val result = videos.single()
            chosen = result
            return result
        } finally { videos.filter { it !== chosen }.forEach { it.clear() } }
    }

    @OptIn(DelicateCryptographyApi::class)
    private suspend fun unwrap(value: String, owner: String, secret: ByteArray): ByteArray {
        val wrapped = value.split('/').singleOrNull { it.substringBefore(':') == owner }
            ?.substringAfter(':') ?: error("MEGA node has no public-folder key")
        val bytes = MegaLink.decode(wrapped)
        require(bytes.size == 32) { "MEGA file key has an invalid length" }
        val key = megaCryptoProvider().get(AES.CBC).keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, secret)
        val plain = ByteArray(32)
        // MEGA wraps node keys with ECB. Independent zero-IV CBC blocks are equivalent and supported on iOS.
        for (offset in listOf(0, 16)) key.cipher(padding = false)
            .decryptWithIv(ByteArray(16), bytes.copyOfRange(offset, offset + 16)).copyInto(plain, offset)
        return plain
    }
}
