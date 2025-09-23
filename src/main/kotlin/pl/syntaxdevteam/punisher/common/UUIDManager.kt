package pl.syntaxdevteam.punisher.common

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX
import java.time.Duration
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class UUIDManager(private val plugin: PunisherX) {
    private data class CachedUuid(val value: UUID, val expiresAt: Long) {
        fun isExpired(now: Long): Boolean = now > expiresAt
    }

    private val cacheTtl = Duration.ofHours(6).toMillis()
    private val activeUUIDs: MutableMap<String, CachedUuid> = ConcurrentHashMap()
    private val gson = Gson()

    fun getUUID(playerName: String): UUID {
        val now = System.currentTimeMillis()
        val key = playerName.lowercase(Locale.ROOT)
        activeUUIDs[key]?.takeUnless { it.isExpired(now) }?.let { return it.value }
        val player: Player? = Bukkit.getPlayer(playerName)
        if (player != null) {
            return remember(key, player.uniqueId)
        }
        val offlinePlayer: OfflinePlayer = Bukkit.getOfflinePlayer(playerName)
        if (offlinePlayer.hasPlayedBefore()) {
            return remember(key, offlinePlayer.uniqueId)
        }
        val uuid = fetchUUIDFromAPI(playerName)
            ?: fetchUUIDFromPlayerDB(playerName)
            ?: generateOfflineUUID(playerName)
        return remember(key, uuid)
    }

    fun getCachedUUID(playerName: String): UUID? {
        val now = System.currentTimeMillis()
        val key = playerName.lowercase(Locale.ROOT)
        return activeUUIDs[key]?.takeUnless { it.isExpired(now) }?.value
    }

    private fun fetchUUIDFromAPI(playerName: String): UUID? {

        val uri = URI("https://api.mojang.com/users/profiles/minecraft/${playerName}")
        val offlineUUID: UUID = UUID.nameUUIDFromBytes("OfflinePlayer:$playerName".toByteArray(Charsets.UTF_8))
        return try {
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            plugin.logger.debug("API Response Code: ${connection.responseCode}")

            if (connection.responseCode == 200) {
                val reader = InputStreamReader(connection.inputStream)
                val response = reader.readText()
                reader.close()

                val uuid = parseUUIDFromResponse(response)
                plugin.logger.debug("parseUUIDFromResponse(response): $uuid")
                uuid
            } else {
                plugin.logger.err("Failed to fetch UUID from API. Response code: ${connection.responseCode}")
                plugin.logger.debug("Returning offline UUID: $offlineUUID")
                offlineUUID
            }
        } catch (e: Exception) {
            plugin.logger.err("Error: $e")
            plugin.logger.debug("Returning offline UUID: $offlineUUID")
            offlineUUID
        }
    }

    private fun fetchUUIDFromPlayerDB(playerName: String): UUID? {
        val uri = URI("https://playerdb.co/api/player/minecraft/$playerName")
        return try {
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()
            plugin.logger.debug("API Response Code: ${connection.responseCode}")
            if (connection.responseCode == 200) {
                val reader = InputStreamReader(connection.inputStream)
                val response = reader.readText()
                reader.close()
                val jsonObject = gson.fromJson(response, JsonObject::class.java)
                val rawUUID = jsonObject.getAsJsonObject("data").getAsJsonObject("player").get("id").asString
                plugin.logger.debug("Raw UUID from PlayerDB API: $rawUUID")
                val uuid = UUID.fromString(rawUUID.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)".toRegex(),
                    "$1-$2-$3-$4-$5"
                ))
                uuid
            } else {
                plugin.logger.err("Failed to fetch UUID from PlayerDB API. Response code: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            plugin.logger.err("Error: $e")
            null
        }
    }

    private fun parseUUIDFromResponse(response: String): UUID? {
        return try {
            val jsonObject = gson.fromJson(response, JsonObject::class.java)
            val rawUUID = jsonObject.get("id").asString
            plugin.logger.debug("Raw UUID from API: $rawUUID")
            UUID.fromString(rawUUID.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)".toRegex(),
                "$1-$2-$3-$4-$5"
            ))
        } catch (e: Exception) {
            plugin.logger.err("Error: $e")
            null
        }
    }

    private fun generateOfflineUUID(playerName: String): UUID {
        val offlineUUID = UUID.nameUUIDFromBytes("OfflinePlayer:$playerName".toByteArray())
        plugin.logger.debug("Generated offline UUID for $playerName: $offlineUUID")
        return offlineUUID
    }

    private fun remember(key: String, uuid: UUID): UUID {
        activeUUIDs[key] = CachedUuid(uuid, System.currentTimeMillis() + cacheTtl)
        return uuid
    }
}
