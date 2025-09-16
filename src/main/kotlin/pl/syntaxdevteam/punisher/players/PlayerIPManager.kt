package pl.syntaxdevteam.punisher.players

import pl.syntaxdevteam.punisher.PunisherX
import org.bukkit.event.player.PlayerJoinEvent
import java.io.File
import java.security.Key
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8

class PlayerIPManager(private val plugin: PunisherX, val geoIPHandler: GeoIPHandler) {

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

    private val useDatabase = plugin.config.getString("playerCache.storage")
        ?.equals("database", ignoreCase = true) == true

    private val separator = "|"

    init {
        if (!useDatabase && !cacheFile.exists()) {
            cacheFile.parentFile.mkdirs()
            cacheFile.createNewFile()
        }
    }

    fun handlePlayerJoin(event: PlayerJoinEvent) {
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
        appendLine(encryptedData)
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

    fun getAllDecryptedRecords(): List<PlayerInfo> {
        return readLines().mapNotNull { line ->
            try {
                val decrypted = decrypt(line)
                parsePlayerInfo(decrypted)
            } catch (e: Exception) {
                plugin.logger.err("Error decrypting line: $line -> $e")
                null
            }
        }
    }

    fun getPlayerIPByName(playerName: String): String? {
        plugin.logger.debug("Fetching IP for player: $playerName")
        val info = searchCache(playerName, "", "")
        val ip = info?.playerIP
        plugin.logger.debug("Found IP for player $playerName: $ip")
        return ip
    }

    fun getPlayerIPByUUID(playerUUID: String): String? {
        plugin.logger.debug("Fetching IP for UUID: $playerUUID")
        val info = searchCache("", playerUUID, "")
        val ip = info?.playerIP
        plugin.logger.debug("Found IP for UUID $playerUUID: $ip")
        return ip
    }

    fun getPlayerIPsByName(playerName: String): List<String> {
        plugin.logger.debug("Fetching all IPs for player: $playerName")
        return getAllDecryptedRecords()
            .filter { it.playerName.equals(playerName, ignoreCase = true) }
            .map { it.playerIP }
            .also { plugin.logger.debug("Found IPs for player $playerName: $it") }
    }

    fun getPlayerIPsByUUID(playerUUID: String): List<String> {
        plugin.logger.debug("Fetching all IPs for UUID: $playerUUID")
        return getAllDecryptedRecords()
            .filter { it.playerUUID.equals(playerUUID, ignoreCase = true) }
            .map { it.playerIP }
            .also { plugin.logger.debug("Found IPs for UUID $playerUUID: $it") }
    }

    fun deletePlayerInfo(playerUUID: UUID) {
        val lines = readLines()
        val filtered = lines.filter { line ->
            val info = parsePlayerInfo(decrypt(line))
            info?.playerUUID != playerUUID.toString()
        }
        cacheFile.writeText("")
        filtered.forEach { cacheFile.appendText("$it\n") }
        plugin.logger.debug("Removed player info for UUID: $playerUUID")
    }

    private fun searchCache(playerName: String, playerUUID: String, playerIP: String): PlayerInfo? {
        plugin.logger.debug("Searching cache")
        val lines = readLines()
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


    fun getPlayersByIP(targetIP: String): List<PlayerInfo> {
        return getAllDecryptedRecords().filter { it.playerIP.equals(targetIP, ignoreCase = true) }
    }

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

    private fun readLines(): List<String> =
        if (useDatabase) plugin.databaseHandler.getPlayerCacheLines() else cacheFile.readLines()

    private fun appendLine(encryptedData: String) {
        if (useDatabase) plugin.databaseHandler.savePlayerCacheLine(encryptedData)
        else cacheFile.appendText("$encryptedData\n")
    }

    private fun overwriteLines(lines: List<String>) {
        if (useDatabase) plugin.databaseHandler.overwritePlayerCache(lines)
        else {
            cacheFile.writeText("")
            lines.forEach { cacheFile.appendText("$it\n") }
        }
    }
}
