package pl.syntaxdevteam.players

import pl.syntaxdevteam.PunisherX
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.io.File
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8

class PlayerIPManager(private val plugin: PunisherX, val geoIPHandler: GeoIPHandler) : Listener {

    private val cacheFile = File(plugin.dataFolder, "cache")
    private val secretKey: Key = generateKey()

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
        val playerIP = "89.64.104.144" //player.address?.address?.hostAddress

        if (playerIP != null) {
            val country = geoIPHandler.getCountry(playerIP)
            val city = geoIPHandler.getCity(playerIP)
            val geoLocation = "$city, $country"
            if (!isPlayerInfoExists(playerName, playerUUID, playerIP)) {
                savePlayerInfo(playerName, playerUUID, playerIP, geoLocation)
                plugin.logger.debug("Saved player info -> playerName: $playerName, playerUUID: $playerUUID, playerIP: $playerIP, geoLocation: $geoLocation")
            } else {
                plugin.logger.debug("Player info already exists -> playerName: $playerName, playerUUID: $playerUUID, playerIP: $playerIP, geoLocation: $geoLocation")
            }
        }
    }

    private fun isPlayerInfoExists(playerName: String, playerUUID: String, playerIP: String): Boolean {
        return searchCache(playerName, playerUUID, playerIP) != null
    }

    private fun savePlayerInfo(playerName: String, playerUUID: String, playerIP: String, geoLocation: String?) {
        val encryptedData = encrypt("$playerName,$playerUUID,$playerIP,$geoLocation")
        cacheFile.appendText("$encryptedData\n")
        plugin.logger.debug("Encrypted data saved -> playerName: $playerName, playerUUID: $playerUUID, playerIP: $playerIP, geoLocation: $geoLocation")
    }

    private fun generateKey(): Key {
        val keyString = "M424PmX84WlDDXLb" // Stały klucz szyfrowania (16 znaków dla AES-128)
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
        } catch (e: NumberFormatException) {
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

    private fun searchCache(playerName: String, playerUUID: String, playerIP: String): String? {
        plugin.logger.debug("Searching cache")
        val lines = cacheFile.readLines()
        plugin.logger.debug("Number of lines in cache: ${lines.size}")
        for (line in lines) {
            val decryptedLine = decrypt(line)
            plugin.logger.debug("Decrypted line: $decryptedLine")
            val parts = decryptedLine.split(",").map { it.trim().lowercase() }
            plugin.logger.debug("Split parts: $parts")
            if ((playerName.isEmpty() || parts[0] == playerName.lowercase()) &&
                (playerUUID.isEmpty() || parts[1] == playerUUID.lowercase()) &&
                (playerIP.isEmpty() || parts[2] == playerIP.lowercase())) {
                plugin.logger.debug("Match found: ${parts[2]}")
                return parts[2]
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
