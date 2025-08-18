package pl.syntaxdevteam.punisher.basic

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Cache
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bukkit.Bukkit
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
// TODO: Zaktualizować komunikaty do nowego systemu
class PunishmentCache(private val plugin: PunisherX) {

    private val cacheFile = File(plugin.dataFolder, "jail_cache.json")

    companion object {
        private const val DEFAULT_EXPIRE_AFTER_ACCESS = 1L     // czas
        private val DEFAULT_EXPIRE_UNIT = TimeUnit.HOURS       // jednostka (możesz to zmienić)
        private const val DEFAULT_MAXIMUM_SIZE = 1_000L        // maksymalna liczba wpisów
    }

    private val cache: Cache<UUID, Long> = Caffeine.newBuilder()
        .expireAfterAccess(DEFAULT_EXPIRE_AFTER_ACCESS, DEFAULT_EXPIRE_UNIT)
        .maximumSize(DEFAULT_MAXIMUM_SIZE)
        .build()

    private val gson = Gson()

    init {
        loadCacheIntoMemory()
    }

    fun addOrUpdatePunishment(uuid: UUID, endTime: Long) {
        val previous = cache.getIfPresent(uuid)
        if (previous == null || previous < System.currentTimeMillis()) {
            plugin.logger.debug("Dodaję nową karę dla gracza $uuid do $endTime")
        } else {
            plugin.logger.debug("Aktualizuję karę dla gracza $uuid z $previous na $endTime")
        }
        cache.put(uuid, endTime)
        saveSingleEntry(uuid, endTime)
    }

    fun removePunishment(uuid: UUID) {
        if (cache.asMap().remove(uuid) != null) {
            plugin.logger.debug("Usuwam karę dla $uuid")
            removeSingleEntry(uuid)
            plugin.databaseHandler.removePunishment(uuid.toString(), "JAIL")

            Bukkit.getPlayer(uuid)?.let { player ->
                val bc = plugin.messageHandler.getSmartMessage("unjail", "broadcast", mapOf("player" to player.name))
                plugin.server.onlinePlayers
                    .filter { it.hasPermission("punisherx.see.unjail") }
                    .forEach { p -> bc.forEach { p.sendMessage(it) } }
                plugin.messageHandler.getSmartMessage(
                    "unjail",
                    "success",
                    mapOf("player" to player.name)
                ).forEach { msg -> player.sendMessage(msg) }
            }
        }
    }

    fun isPunishmentActive(uuid: UUID): Boolean {
        return cache.getIfPresent(uuid)?.let { it > System.currentTimeMillis() } ?: false
    }

    fun isPlayerInCache(uuid: UUID): Boolean =
        cache.asMap().containsKey(uuid)

    fun getPunishmentEnd(uuid: UUID): Long? =
        cache.getIfPresent(uuid)

    fun getActivePunishments(): Map<UUID, Long> =
        cache.asMap().filterValues { it > System.currentTimeMillis() }

    private fun loadCacheIntoMemory() {
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
                    cache.put(UUID.fromString(key), value)
                } else {
                    plugin.logger.warning("Invalid UUID in cache: $key. Skipping this entry.")
                }
            }
            plugin.logger.debug("Loaded ${cache.asMap().size} entries into cache.")
        } catch (e: Exception) {
            plugin.logger.severe("Unexpected error loading cache: ${e.message}")
        }
    }

    private fun isValidUUID(str: String): Boolean =
        try {
            UUID.fromString(str)
            true
        } catch (_: IllegalArgumentException) {
            false
        }

    private fun saveSingleEntry(uuid: UUID, endTime: Long) {
        try {
            val existing = if (cacheFile.exists()) {
                gson.fromJson<MutableMap<String, Long>>(cacheFile.readText(), object : TypeToken<MutableMap<String, Long>>() {}.type)
                    ?: mutableMapOf()
            } else {
                mutableMapOf()
            }
            existing[uuid.toString()] = endTime
            cacheFile.writeText(gson.toJson(existing))
            plugin.logger.debug("Saved punishment for player $uuid.")
        } catch (e: IOException) {
            plugin.logger.severe("Error saving punishment to file: ${e.message}")
        }
    }

    private fun removeSingleEntry(uuid: UUID) {
        try {
            if (!cacheFile.exists()) return
            val existing = gson.fromJson<MutableMap<String, Long>>(cacheFile.readText(), object : TypeToken<MutableMap<String, Long>>() {}.type)
                ?: mutableMapOf()
            existing.remove(uuid.toString())
            cacheFile.writeText(gson.toJson(existing))
            plugin.logger.debug("Removed punishment for player $uuid from file.")
        } catch (e: IOException) {
            plugin.logger.severe("Error removing punishment from file: ${e.message}")
        }
    }
}
