package pl.syntaxdevteam.helpers

import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.util.*
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import pl.syntaxdevteam.PunisherX

class UUIDManager(private val plugin: PunisherX) {
    private val activeUUIDs: MutableMap<String, UUID> = HashMap()

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
        return uuid ?: generateOfflineUUID(playerName)
    }

    private fun fetchUUIDFromAPI(playerName: String): UUID? {
        val uri = URI("https://api.mojang.com/users/profiles/minecraft/$playerName")
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
                if (uuid != null) {
                    activeUUIDs[playerName.lowercase(Locale.getDefault())] = uuid
                }
                uuid
            } else {
                plugin.logger.err("Failed to fetch UUID from API. Response code: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            plugin.logger.err("Error: $e")
            null
        }
    }

    private fun parseUUIDFromResponse(response: String): UUID? {
        return try {
            val parser = JSONParser()
            val jsonObject = parser.parse(response) as JSONObject
            val rawUUID = jsonObject["id"] as String
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
        println("Generated offline UUID for $playerName: $offlineUUID")
        return offlineUUID
    }
}
