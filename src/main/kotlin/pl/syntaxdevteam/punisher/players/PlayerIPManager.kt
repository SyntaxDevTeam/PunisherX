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

    private val cacheFile = File(plugin.dataFolder, "cache")
    private val secretKey: Key = generateKey()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

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

            if (!isPlayerInfoExists(playerName, playerUUID, playerIP)) {
                savePlayerInfo(playerName, playerUUID, playerIP, geoLocation, lastUpdated)
                plugin.logger.debug("Saved player info -> playerName: $playerName, playerUUID: $playerUUID, playerIP: $playerIP, geoLocation: $geoLocation, lastUpdated: $lastUpdated")
            } else {
                plugin.logger.debug("Player info already exists -> playerName: $playerName, playerUUID: $playerUUID, playerIP: $playerIP, geoLocation: $geoLocation, lastUpdated: $lastUpdated")
            }
        }
    }

    private fun isPlayerInfoExists(playerName: String, playerUUID: String, playerIP: String): Boolean {
        return searchCache(playerName, playerUUID, playerIP) != null
    }

    private fun savePlayerInfo(playerName: String, playerUUID: String, playerIP: String, geoLocation: String?, lastUpdated: String) {
        val encryptedData = encrypt("$playerName,$playerUUID,$playerIP,$geoLocation,$lastUpdated")
        cacheFile.appendText("$encryptedData\n")
        plugin.logger.debug("Encrypted data saved -> playerName: $playerName, playerUUID: $playerUUID, playerIP: $playerIP, geoLocation: $geoLocation, lastUpdated: $lastUpdated")
    }

    private fun generateKey(): Key {
        val keyString = "M424PmX84WlDDXLb" // Fixed encryption key (16 characters for AES-128) if you are using the developer version remember to change it to your own
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

    fun getPlayerIPByName(playerName: String): String? {
        plugin.logger.debug("Fetching IP for player: $playerName")
        val ip = searchCache(playerName, "", "")
        plugin.logger.debug("Found IP for player $playerName: $ip")
        return ip
    }

    fun getPlayerIPByUUID(playerUUID: String): String? {
        plugin.logger.debug("Fetching IP for UUID: $playerUUID")
        val ip = searchCache("", playerUUID, "")
        plugin.logger.debug("Found IP for UUID $playerUUID: $ip")
        return ip
    }

    fun getAllDecryptedRecords(): List<String> {
        return cacheFile.readLines().mapNotNull { line ->
            try {
                decrypt(line)
            } catch (e: Exception) {
                plugin.logger.err("Error decrypting line: $line -> $e")
                null
            }
        }
    }

    private fun searchCache(playerName: String, playerUUID: String, playerIP: String): String? {
        plugin.logger.debug("Searching cache")
        val lines = cacheFile.readLines()
        plugin.logger.debug("Number of lines in cache: ${lines.size}")
        for (line in lines) {
            val decryptedLine = decrypt(line)
            plugin.logger.debug("Decrypted line: $decryptedLine")
            val parts = decryptedLine.split(",").map { it.trim() }
            if ((playerName.isEmpty() || parts[0].equals(playerName, ignoreCase = true)) &&
                (playerUUID.isEmpty() || parts[1].equals(playerUUID, ignoreCase = true)) &&
                (playerIP.isEmpty() || parts[2].equals(playerIP, ignoreCase = true))
            ) {
                plugin.logger.debug("Match found: $decryptedLine")
                return decryptedLine
            }
        }
        plugin.logger.debug("No match found in cache")
        return null
    }
    /*
        fun getPlayerNamesByIP(playerIP: String): List<String> {
            return searchCacheMultiple { it[2] == playerIP }
        }

        private fun searchCacheMultiple(predicate: (List<String>) -> Boolean): List<String> {
            return cacheFile.readLines().map { decrypt(it).split(",") }.filter(predicate).map { it[0] }
        }
    */
}
