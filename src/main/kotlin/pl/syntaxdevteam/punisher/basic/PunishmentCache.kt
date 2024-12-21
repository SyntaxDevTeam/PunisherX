package pl.syntaxdevteam.punisher.basic

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
            plugin.logger.debug("Dodaję nową karę dla gracza $uuid with end time $endTime")
        } else {
            plugin.logger.debug("Aktualizuję karę dla gracza $uuid z $currentEndTime na $endTime")
        }

        cache[uuid] = endTime
        saveSingleEntry(uuid, endTime)
    }

    fun removePunishment(uuid: UUID) {
        if (cache.remove(uuid) != null) {
            plugin.logger.debug("Usunięto karę dla gracza $uuid")
            removeSingleEntry(uuid)
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
            plugin.logger.debug("Plik cache nie istnieje, tworzę pusty cache.")
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
                    plugin.logger.warning("Nieprawidłowy UUID w cache: $key. Pomijam ten wpis.")
                }
            }
            plugin.logger.debug("Wczytano ${cache.size} wpisów do cache.")
        } catch (e: IOException) {
            plugin.logger.severe("Błąd podczas wczytywania cache: ${e.message}")
        } catch (e: Exception) {
            plugin.logger.severe("Nieoczekiwany błąd podczas wczytywania cache: ${e.message}")
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
            plugin.logger.debug("Zapisano karę dla gracza $uuid.")
        } catch (e: IOException) {
            plugin.logger.severe("Błąd podczas zapisywania kary do pliku: ${e.message}")
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
            plugin.logger.debug("Usunięto karę dla gracza $uuid z pliku.")
        } catch (e: IOException) {
            plugin.logger.severe("Błąd podczas usuwania kary z pliku: ${e.message}")
        }
    }
}
