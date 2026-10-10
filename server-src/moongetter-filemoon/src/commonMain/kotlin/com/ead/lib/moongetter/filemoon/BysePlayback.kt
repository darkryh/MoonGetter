package com.ead.lib.moongetter.filemoon

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64

/** Decodes the authenticated payload returned by the Byse playback API. */
internal object BysePlayback {
    @OptIn(DelicateCryptographyApi::class)
    suspend fun decode(payload: JsonObject): JsonObject {
        require(payload["algorithm"]?.jsonPrimitive?.content in listOf("AES-GCM", "AES-256-GCM")) { "Unsupported Byse encryption" }
        val parts = payload.getValue("key_parts").jsonArray
        require(parts.size in 1..64) { "Invalid Byse key parts" }
        val version = payload["version"]?.jsonPrimitive?.content?.toIntOrNull()
        val indices = if (version != null && version in 1..20 && version <= parts.size && 31 - version <= parts.size)
            listOf(version - 1, 30 - version) else parts.indices.toList()
        val bytes = indices.map { base64(parts[it].jsonPrimitive.content) }
        val key = bytes.fold(byteArrayOf()) { accumulated, part -> accumulated + part }
        require(key.size in listOf(16, 24, 32) && (payload["algorithm"]?.jsonPrimitive?.content != "AES-256-GCM" || key.size == 32)) { "Invalid Byse AES key" }
        val iv = base64(payload.getValue("iv").jsonPrimitive.content)
        require(iv.size == 12) { "Invalid Byse IV" }
        val ciphertext = base64(payload.getValue("payload").jsonPrimitive.content)
        require(ciphertext.size in 16..1_048_576) { "Invalid Byse ciphertext" }
        val decodedKey = CryptographyProvider.Default.get(AES.GCM).keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, key)
        val cleartext = decodedKey.cipher().decryptWithIv(iv, ciphertext)
        return Json.parseToJsonElement(cleartext.decodeToString(throwOnInvalidSequence = true)).jsonObject
    }

    private fun base64(value: String): ByteArray {
        require(value.length <= 1_400_000)
        val normalized = value.replace('-', '+').replace('_', '/')
        return Base64.Default.decode(normalized + "=".repeat((4 - normalized.length % 4) % 4))
    }
}
