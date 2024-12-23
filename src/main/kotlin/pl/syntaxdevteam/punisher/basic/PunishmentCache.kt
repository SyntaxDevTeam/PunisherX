package pl.syntaxdevteam.punisher.basic

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PunishmentCache(private val plugin: PunisherX) {

    private val cacheFile: File = File(plugin.dataFolder, "jail_cache.json")
    private val cache: MutableMap<UUID, Long> = ConcurrentHashMap()
    private val gson: Gson = Gson()

    init {
        loadCache()
    }

    fun addOrUpdatePunishment(uuid: UUID, endTime: Long) {
        val currentEndTime = cache[uuid]
        if (currentEndTime == null || currentEndTime < System.currentTimeMillis()) {
            plugin.logger.debug("Adding new punishment for player $uuid with end time $endTime")
        } else {
            plugin.logger.debug("Updating punishment for player $uuid from $currentEndTime to $endTime")
        }

        cache[uuid] = endTime
        saveSingleEntry(uuid, endTime)
    }

    fun removePunishment(uuid: UUID) {
        if (cache.remove(uuid) != null) {
            plugin.logger.debug("Removed punishment for player $uuid")
            removeSingleEntry(uuid)
            plugin.databaseHandler.removePunishment(uuid.toString(), "JAIL")
            val player = Bukkit.getPlayer(uuid)
            if (player != null) {
                val broadcastMessage = MiniMessage.miniMessage().deserialize(
                    plugin.messageHandler.getMessage("unjail", "broadcast", mapOf("player" to player.name))
                )
                plugin.server.onlinePlayers.forEach { onlinePlayer ->
                    if (onlinePlayer.hasPermission("punisherx.see.unjail")) {
                        onlinePlayer.sendMessage(broadcastMessage)
                    }
                }

                player.sendRichMessage(
                    plugin.messageHandler.getMessage("unjail", "success", mapOf("player" to player.name))
                )
            }
        }
    }

    @Suppress("unused")
    fun isPunishmentActive(uuid: UUID): Boolean {
        val endTime = cache[uuid] ?: return false
        return endTime > System.currentTimeMillis()
    }

    fun isPlayerInCache(uuid: UUID): Boolean {
        return cache.containsKey(uuid)
    }

    fun getPunishmentEnd(uuid: UUID): Long? {
        return cache[uuid]
    }

    private fun loadCache() {
        if (!cacheFile.exists()) {
            plugin.logger.debug("Cache file does not exist, creating an empty cache.")
            return
        }

        try {
            val json = cacheFile.readText()
            val type = object : TypeToken<Map<String, Long>>() {}.type
            val map: Map<String, Long> = gson.fromJson(json, type)
            map.forEach { (key, value) ->
                if (isValidUUID(key)) {
                    cache[UUID.fromString(key)] = value
                } else {
                    plugin.logger.warning("Invalid UUID in cache: $key. Skipping this entry.")
                }
            }
            plugin.logger.debug("Loaded ${cache.size} entries into cache.")
        } catch (e: IOException) {
            plugin.logger.severe("Error loading cache: ${e.message}")
        } catch (e: Exception) {
            plugin.logger.severe("Unexpected error loading cache: ${e.message}")
        }
    }

    private fun isValidUUID(uuid: String): Boolean {
        return try {
            UUID.fromString(uuid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun saveSingleEntry(uuid: UUID, endTime: Long) {
        try {
            val fileContent = if (cacheFile.exists()) cacheFile.readText() else "{}"
            val type = object : TypeToken<MutableMap<String, Long>>() {}.type
            val existingData: MutableMap<String, Long> = gson.fromJson(fileContent, type) ?: mutableMapOf()
            existingData[uuid.toString()] = endTime
            cacheFile.writeText(gson.toJson(existingData))
            plugin.logger.debug("Saved punishment for player $uuid.")
        } catch (e: IOException) {
            plugin.logger.severe("Error saving punishment to file: ${e.message}")
        }
    }

    private fun removeSingleEntry(uuid: UUID) {
        try {
            if (!cacheFile.exists()) return
            val fileContent = cacheFile.readText()
            val type = object : TypeToken<MutableMap<String, Long>>() {}.type
            val existingData: MutableMap<String, Long> = gson.fromJson(fileContent, type) ?: mutableMapOf()
            existingData.remove(uuid.toString())
            cacheFile.writeText(gson.toJson(existingData))
            plugin.logger.debug("Removed punishment for player $uuid from file.")
        } catch (e: IOException) {
            plugin.logger.severe("Error removing punishment from file: ${e.message}")
        }
    }
}