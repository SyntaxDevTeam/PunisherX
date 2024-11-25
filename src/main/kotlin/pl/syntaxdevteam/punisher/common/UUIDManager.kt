package pl.syntaxdevteam.punisher.common

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.util.*
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import pl.syntaxdevteam.punisher.PunisherX

class UUIDManager(private val plugin: PunisherX) {
    private val activeUUIDs: MutableMap<String, UUID> = HashMap()
    private val gson = Gson()

    fun getUUID(playerName: String): UUID {
        val player: Player? = Bukkit.getPlayer(playerName)
        if (player != null) {
            return player.uniqueId
        }
        val offlinePlayer: OfflinePlayer = Bukkit.getOfflinePlayer(playerName)
        if (offlinePlayer.hasPlayedBefore()) {
            return offlinePlayer.uniqueId
        }
        val uuid = fetchUUIDFromAPI(playerName)
            ?: fetchUUIDFromPlayerDB(playerName)
            ?: fetchUUIDFromNameMC(playerName)
            ?: generateOfflineUUID(playerName)
        return uuid
    }

    private fun fetchUUIDFromAPI(playerName: String): UUID? {
        val player: Player? = Bukkit.getPlayer(playerName)
        val uri = URI("https://api.mojang.com/users/profiles/minecraft/${player?.name}")
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
                if (uuid != null) {
                    activeUUIDs[playerName.lowercase(Locale.getDefault())] = uuid
                }
                uuid
            } else {
                plugin.logger.err("Failed to fetch UUID from API. Response code: ${connection.responseCode}")
                player?.uniqueId
            }
        } catch (e: Exception) {
            plugin.logger.err("Error: $e")
            player?.uniqueId
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
                if (uuid != null) {
                    activeUUIDs[playerName.lowercase(Locale.getDefault())] = uuid
                }
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

    private fun fetchUUIDFromNameMC(playerName: String): UUID? {
        val uri = URI("https://api.namemc.com/profile/$playerName")
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
                val rawUUID = jsonObject.get("id").asString
                plugin.logger.debug("Raw UUID from NameMC API: $rawUUID")
                val uuid = UUID.fromString(rawUUID.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)".toRegex(),
                    "$1-$2-$3-$4-$5"
                ))
                if (uuid != null) {
                    activeUUIDs[playerName.lowercase(Locale.getDefault())] = uuid
                }
                uuid
            } else {
                plugin.logger.err("Failed to fetch UUID from NameMC API. Response code: ${connection.responseCode}")
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
}
