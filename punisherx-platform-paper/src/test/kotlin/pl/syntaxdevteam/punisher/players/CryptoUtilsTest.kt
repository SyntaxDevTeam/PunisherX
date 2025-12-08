package pl.syntaxdevteam.punisher.players

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CryptoUtilsTest {

    @Test
    fun `toHexString encodes bytes using two characters`() {
        val bytes = ByteArray(256) { index -> index.toByte() }

        val hex = bytes.toHexString()

        assertEquals(bytes.size * 2, hex.length, "Each byte should become two hex characters")
        assertEquals(hex.lowercase(), hex, "Encoding should use lowercase characters")

        val decoded = hexStringToByteArray(hex)
        assertContentEquals(bytes, decoded)
    }

    @Test
    fun `hexStringToByteArray rejects odd length input`() {
        assertFailsWith<IllegalArgumentException> {
            hexStringToByteArray("abc")
        }
    }
}