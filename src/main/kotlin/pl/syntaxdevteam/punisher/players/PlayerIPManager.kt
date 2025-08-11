package pl.syntaxdevteam.punisher.players

import pl.syntaxdevteam.punisher.PunisherX
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.io.File
import java.security.Key
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8

class PlayerIPManager(private val plugin: PunisherX, val geoIPHandler: GeoIPHandler) : Listener {

    // Data class reprezentująca pojedynczy wpis
    data class PlayerInfo(
        val playerName: String,
        val playerUUID: String,
        val playerIP: String,
        val geoLocation: String,
        val lastUpdated: String
    )

    private val cacheFile = File(plugin.dataFolder, "cache")
    private val secretKey: Key = generateKey()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    // Ustalony separator – używamy znaku, który nie występuje w danych
    private val separator = "|"

    init {
        if (!cacheFile.exists()) {
            cacheFile.parentFile.mkdirs()
            cacheFile.createNewFile()
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val playerName = player.name
        val playerUUID = player.uniqueId.toString()
        val playerIP = player.address?.address?.hostAddress

        if (playerIP != null) {
            val country = geoIPHandler.getCountry(playerIP)
            val city = geoIPHandler.getCity(playerIP)
            val geoLocation = "$city, $country"
            val lastUpdated = dateFormat.format(Date())

            if (getPlayerInfo(playerName, playerUUID, playerIP) == null) {
                savePlayerInfo(playerName, playerUUID, playerIP, geoLocation, lastUpdated)
                plugin.logger.debug("Saved player info -> playerName: $playerName, playerUUID: $playerUUID, playerIP: $playerIP, geoLocation: $geoLocation, lastUpdated: $lastUpdated")
            } else {
                plugin.logger.debug("Player info already exists -> playerName: $playerName, playerUUID: $playerUUID, playerIP: $playerIP, geoLocation: $geoLocation, lastUpdated: $lastUpdated")
            }
        }
    }

    private fun getPlayerInfo(playerName: String, playerUUID: String, playerIP: String): PlayerInfo? {
        return searchCache(playerName, playerUUID, playerIP)
    }

    private fun savePlayerInfo(playerName: String, playerUUID: String, playerIP: String, geoLocation: String, lastUpdated: String) {

        val dataLine = "$playerName$separator$playerUUID$separator$playerIP$separator$geoLocation$separator$lastUpdated"
        val encryptedData = encrypt(dataLine)
        cacheFile.appendText("$encryptedData\n")
        plugin.logger.debug("Encrypted data saved -> $dataLine")
    }

    private fun generateKey(): Key {
        val keyString = System.getenv("AES_KEY") ?: "1234567890ABCDEF" // test fallback

        require(keyString.length == 16) { "AES key must be exactly 16 characters long" }
        return SecretKeySpec(keyString.toByteArray(UTF_8), "AES")
    }


    private fun encrypt(data: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(data.toByteArray(UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun decrypt(data: String): String {
        return try {
            val bytes = data.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            String(cipher.doFinal(bytes), UTF_8)
        } catch (e: Exception) {
            plugin.logger.err("Failed to decrypt data: $data -> $e")
            ""
        }
    }

    // Zwraca listę rekordów jako obiekty PlayerInfo
    fun getAllDecryptedRecords(): List<PlayerInfo> {
        return cacheFile.readLines().mapNotNull { line ->
            try {
                val decrypted = decrypt(line)
                parsePlayerInfo(decrypted)
            } catch (e: Exception) {
                plugin.logger.err("Error decrypting line: $line -> $e")
                null
            }
        }
    }

    // Pobiera tylko IP na podstawie nazwy gracza – wyodrębnia odpowiednie pole
    fun getPlayerIPByName(playerName: String): String? {
        plugin.logger.debug("Fetching IP for player: $playerName")
        val info = searchCache(playerName, "", "")
        val ip = info?.playerIP
        plugin.logger.debug("Found IP for player $playerName: $ip")
        return ip
    }

    // Pobiera tylko IP na podstawie UUID
    fun getPlayerIPByUUID(playerUUID: String): String? {
        plugin.logger.debug("Fetching IP for UUID: $playerUUID")
        val info = searchCache("", playerUUID, "")
        val ip = info?.playerIP
        plugin.logger.debug("Found IP for UUID $playerUUID: $ip")
        return ip
    }

    // Przeszukuje cache i zwraca obiekt PlayerInfo, jeśli znajdzie pasujący rekord
    private fun searchCache(playerName: String, playerUUID: String, playerIP: String): PlayerInfo? {
        plugin.logger.debug("Searching cache")
        val lines = cacheFile.readLines()
        plugin.logger.debug("Number of lines in cache: ${lines.size}")
        for (line in lines) {
            val decryptedLine = decrypt(line)
            plugin.logger.debug("Decrypted line: $decryptedLine")
            val info = parsePlayerInfo(decryptedLine)
            if (info != null &&
                (playerName.isEmpty() || info.playerName.equals(playerName, ignoreCase = true)) &&
                (playerUUID.isEmpty() || info.playerUUID.equals(playerUUID, ignoreCase = true)) &&
                (playerIP.isEmpty() || info.playerIP.equals(playerIP, ignoreCase = true))
            ) {
                plugin.logger.debug("Match found: $decryptedLine")
                return info
            }
        }
        plugin.logger.debug("No match found in cache")
        return null
    }

    // Pomocnicza metoda do parsowania odszyfrowanego ciągu na obiekt PlayerInfo
    private fun parsePlayerInfo(decryptedLine: String): PlayerInfo? {
        val parts = decryptedLine.split(separator).map { it.trim() }
        return if (parts.size >= 5) {
            PlayerInfo(
                playerName = parts[0],
                playerUUID = parts[1],
                playerIP = parts[2],
                geoLocation = parts[3],
                lastUpdated = parts[4]
            )
        } else {
            plugin.logger.err("Invalid record format: $decryptedLine")
            null
        }
    }
}
