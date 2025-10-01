package pl.syntaxdevteam.punisher.players

internal fun ByteArray.toHexString(): String =
    joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }

internal fun hexStringToByteArray(hex: String): ByteArray {
    require(hex.length % 2 == 0) { "Hex string must have an even length" }

    return ByteArray(hex.length / 2) { index ->
        val start = index * 2
        hex.substring(start, start + 2).toInt(16).toByte()
    }
}