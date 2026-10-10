package com.ead.lib.moongetter.mega
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class MegaTest {
    @Test fun strictPublicLinkAndKey() {
        assertEquals("ABCDEFGH",MegaLink.parse("https://mega.nz/file/ABCDEFGH#AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8").handle)
        assertFails { MegaLink.parse("https://mega.nz.evil/file/ABCDEFGH#AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8") }
        assertFails { MegaLink.parse("https://mega.nz/file/ABCDEFGH") }
    }
    @Test fun cryptoMatchesIndependentFixturesAndSeekOffsets()=runBlocking {
        val link=MegaLink.parse("https://mega.nz/file/ABCDEFGH#AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8")
        val cipher=MegaCipher(link)
        assertEquals("fixture.mp4",cipher.attributes("wpKQwlnQlVnqJllVhq4k3zBTgKSh6w8g94ZOR0VRWJs"))
        val encrypted=MegaLink.decode("nYPaa_MlUlM03KC5ix8zrzumUGMk-qhNjWDTTrK3bZrjsU4Vs-A88ZB3yCgzDd4yAvOxrLqDWs6O8iexXgTNguN0S2L0IXO_Rm6022CiZmLqVxjcVMm3DJYWFlkhXEOD3nmk7-U5RGU_k_XjiWXnWnRNTyuzqb9edYMIyJNbHW4_mrfRL_frHnas-HO2Xj3Fsv0aDE0Lg0UuIU3WaksMlNzSjSe96jtsmYjmM9uBAtgrJ-jCI3Qe2iTCm_wFjcyUGc6iYpJJFUdlEZ4pMEe3E68u1lJZUoUZCOynbyYijF1Sot7ON7iuMnIdAP31OeyJpWzQESpiWfTQc9Atg7KmjQ")
        val plain=cipher.decrypt(0,encrypted)
        assertContentEquals(ByteArray(256) { it.toByte() },plain)
        assertContentEquals(plain.copyOfRange(112,256),cipher.decrypt(112,encrypted.copyOfRange(112,256)))
        assertFails { cipher.decrypt(113,encrypted) }
        link.clear();assertTrue(link.secret.all { it==0.toByte() })
    }
    @Test fun httpRangesCoverSeekingAndInvalidRequests() {
        assertEquals(MegaRange(0,99),MegaRange.parse(null,100))
        assertEquals(MegaRange(10,99),MegaRange.parse("bytes=10-",100))
        assertEquals(MegaRange(95,99),MegaRange.parse("bytes=-5",100))
        assertEquals(MegaRange(0,99),MegaRange.parse("bytes=-1000",100))
        assertEquals(MegaRange(90,99),MegaRange.parse("bytes=90-999",100))
        for(h in listOf("bytes=100-","bytes=9-3","bytes=-0","bytes=0-1,3-4","bytes=999999999999999999999999-","wrong")) assertNull(MegaRange.parse(h,100))
    }
}
