package com.ead.lib.moongetter.mega

import dev.whyoleg.cryptography.*
import dev.whyoleg.cryptography.algorithms.AES
import io.ktor.client.HttpClient
import kotlin.io.encoding.Base64
import kotlinx.serialization.json.*

internal expect fun megaHttpClient(timeout: Long): HttpClient
internal expect fun megaCryptoProvider(): CryptographyProvider

internal data class MegaLink(
    val handle: String,
    val secret: ByteArray,
    val folderHandle: String? = null,
    val expectedName: String? = null,
    val expectedSize: Long? = null
) {
    companion object {
        fun parse(url: String): MegaLink {
            val match=Regex("""^https://mega\.nz/file/([A-Za-z0-9_-]{8})#([A-Za-z0-9_-]{43})$""").matchEntire(url)
                ?: error("Expected a MEGA public file link including its key")
            return MegaLink(match.groupValues[1],decode(match.groupValues[2]).also { require(it.size==32) })
        }
        fun decode(value:String):ByteArray=Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(value)
    }
    val aes:ByteArray get()=ByteArray(16) { (secret[it].toInt() xor secret[it+16].toInt()).toByte() }
    fun clear() { secret.fill(0) }
}

@OptIn(DelicateCryptographyApi::class)
internal class MegaCipher(private val link: MegaLink) {
    private val provider=megaCryptoProvider()
    suspend fun attributes(encoded:String):String {
        require(encoded.length<=16384)
        val bytes=MegaLink.decode(encoded)
        require(bytes.isNotEmpty() && bytes.size%16==0)
        val key=provider.get(AES.CBC).keyDecoder().decodeFromByteArray(AES.Key.Format.RAW,link.aes)
        val plain=key.cipher(padding=false).decryptWithIv(ByteArray(16),bytes).decodeToString(throwOnInvalidSequence=true).trimEnd('\u0000')
        require(plain.startsWith("MEGA")) { "Invalid MEGA decryption key" }
        return Json.parseToJsonElement(plain.substring(4)).jsonObject["n"]!!.jsonPrimitive.content
    }
    suspend fun decrypt(offset:Long,ciphertext:ByteArray):ByteArray {
        require(offset>=0 && offset%16==0L)
        val iv=ByteArray(16)
        link.secret.copyInto(iv,0,16,24)
        var block=offset/16
        for(i in 15 downTo 8) { iv[i]=block.toByte();block=block ushr 8 }
        val key=provider.get(AES.CTR).keyDecoder().decodeFromByteArray(AES.Key.Format.RAW,link.aes)
        return key.cipher().decryptWithIv(iv,ciphertext)
    }
}

internal data class MegaRange(val start:Long,val end:Long) {
    val length:Long get()=end-start+1
    companion object {
        fun parse(header:String?,size:Long):MegaRange? {
            require(size>0)
            if(header==null) return MegaRange(0,size-1)
            val m=Regex("bytes=(\\d*)-(\\d*)").matchEntire(header.trim()) ?: return null
            val a=m.groupValues[1];val b=m.groupValues[2]
            if(a.isEmpty()) { val suffix=b.toLongOrNull()?.takeIf { it>0 } ?: return null;return MegaRange((size-suffix).coerceAtLeast(0),size-1) }
            val start=a.toLongOrNull()?.takeIf { it in 0 until size } ?: return null
            val end=if(b.isEmpty()) size-1 else b.toLongOrNull()?.takeIf { it>=start }?.coerceAtMost(size-1) ?: return null
            return MegaRange(start,end)
        }
    }
}
