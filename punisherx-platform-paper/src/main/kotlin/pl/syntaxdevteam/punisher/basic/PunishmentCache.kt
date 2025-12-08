package pl.syntaxdevteam.punisher.basic

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import pl.syntaxdevteam.punisher.PunisherX
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
// TODO: Zaktualizować komunikaty do nowego systemu
class PunishmentCache(private val plugin: PunisherX) {

    private val cacheFile = File(plugin.dataFolder, "jail_cache.json")

    companion object {
        private const val DEFAULT_EXPIRE_AFTER_ACCESS = 1L     // czas
        private val DEFAULT_EXPIRE_UNIT = TimeUnit.HOURS       // jednostka (możesz to zmienić)
        private const val DEFAULT_MAXIMUM_SIZE = 1_000L        // maksymalna liczba wpisów
    }

    private val cache: Cache<UUID, CachedPunishment> = Caffeine.newBuilder()
        .expireAfterAccess(DEFAULT_EXPIRE_AFTER_ACCESS, DEFAULT_EXPIRE_UNIT)
        .maximumSize(DEFAULT_MAXIMUM_SIZE)
        .build()

    private val gson = Gson()

    init {
        loadCacheIntoMemory()
    }

    fun addOrUpdatePunishment(uuid: UUID, endTime: Long, returnLocation: Location?) {
        val previous = cache.getIfPresent(uuid)
        val now = System.currentTimeMillis()
        val previousActive = previous?.let { it.endTime == -1L || it.endTime > now } ?: false
        if (!previousActive) {
            plugin.logger.debug("Dodaję nową karę dla gracza $uuid do $endTime")
        } else {
            plugin.logger.debug("Aktualizuję karę dla gracza $uuid z ${previous.endTime} na $endTime")
        }

        val storedLocation = returnLocation
            ?.takeIf { it.world != null }
            ?.let { StoredLocation.fromLocation(it) }
            ?: previous?.returnLocation
        val punishment = CachedPunishment(endTime, storedLocation)

        cache.put(uuid, punishment)
        saveSingleEntry(uuid, punishment)
    }

    fun removePunishment(uuid: UUID, teleportPlayer: Boolean = true, notify: Boolean = true) {
        val punishment = cache.getIfPresent(uuid) ?: run {
            removeSingleEntry(uuid)
            return
        }

        cache.invalidate(uuid)
        plugin.logger.debug("Usuwam karę dla $uuid")
        removeSingleEntry(uuid)
        plugin.databaseHandler.removePunishment(uuid.toString(), "JAIL")

        val player = Bukkit.getPlayer(uuid)
        if (player != null) {
            if (teleportPlayer) {
                val cachedLocation = punishment.returnLocation?.toLocation()
                val targetLocation = JailUtils.getUnjailLocation(
                    plugin.config,
                    plugin.hookHandler,
                    cachedLocation,
                    player,
                    plugin.safeTeleportService
                )

                if (targetLocation != null) {
                    plugin.safeTeleportService.teleportSafely(player, targetLocation) { success ->
                        if (success) {
                            player.gameMode = GameMode.SURVIVAL
                            plugin.logger.debug("Przeniesiono gracza ${player.name} po zakończeniu kary na ${targetLocation}")
                        } else {
                            plugin.logger.debug("<red>Nie udało się przenieść gracza ${player.name} po zakończeniu kary.</red>")
                        }
                    }
                } else {
                    plugin.logger.debug("Brak zapisanej lokalizacji powrotu i /setunjail dla gracza ${player.name}")
                }
            }

            if (notify) {
                val bc = plugin.messageHandler.getSmartMessage("unjail", "broadcast", mapOf("player" to player.name))
                plugin.server.onlinePlayers
                    .filter { it.hasPermission("punisherx.see.unjail") }
                    .forEach { p -> bc.forEach { msg -> p.sendMessage(msg) } }
                plugin.messageHandler.getSmartMessage(
                    "unjail",
                    "success",
                    mapOf("player" to player.name)
                ).forEach { msg -> player.sendMessage(msg) }
            }
        }
    }

    fun isPunishmentActive(uuid: UUID): Boolean {
        return cache.getIfPresent(uuid)?.let { it.endTime > System.currentTimeMillis() || it.endTime == -1L } ?: false
    }

    fun isPlayerInCache(uuid: UUID): Boolean =
        cache.asMap().containsKey(uuid)

    fun getPunishmentEnd(uuid: UUID): Long? =
        cache.getIfPresent(uuid)?.endTime

    fun getActivePunishments(): Map<UUID, Long> =
        cache.asMap()
            .filterValues { it.endTime > System.currentTimeMillis() || it.endTime == -1L }
            .mapValues { it.value.endTime }

    fun getReleaseLocation(uuid: UUID): Location? {
        val cached = cache.getIfPresent(uuid)
        val storedLocation = cached?.returnLocation?.toLocation()
        val offlinePlayer = runCatching { Bukkit.getOfflinePlayer(uuid) }.getOrNull()
        return JailUtils.getUnjailLocation(
            plugin.config,
            plugin.hookHandler,
            storedLocation,
            offlinePlayer,
            plugin.safeTeleportService
        )
    }

    private fun loadCacheIntoMemory() {
        if (!cacheFile.exists()) {
            plugin.logger.debug("Cache file does not exist, creating an empty cache.")
            return
        }

        val map = readCacheEntries()
        map.forEach { (key, value) ->
            if (isValidUUID(key)) {
                cache.put(UUID.fromString(key), value)
            } else {
                plugin.logger.warning("Invalid UUID in cache: $key. Skipping this entry.")
            }
        }
        plugin.logger.debug("Loaded ${cache.asMap().size} entries into cache.")
    }

    private fun isValidUUID(str: String): Boolean =
        try {
            UUID.fromString(str)
            true
        } catch (_: IllegalArgumentException) {
            false
        }

    private fun saveSingleEntry(uuid: UUID, punishment: CachedPunishment) {
        try {
            val existing = readCacheEntries()
            existing[uuid.toString()] = punishment
            cacheFile.writeText(gson.toJson(existing))
            plugin.logger.debug("Saved punishment for player $uuid.")
        } catch (e: IOException) {
            plugin.logger.severe("Error saving punishment to file: ${e.message}")
        }
    }

    private fun removeSingleEntry(uuid: UUID) {
        try {
            if (!cacheFile.exists()) return
            val existing = readCacheEntries()
            existing.remove(uuid.toString())
            cacheFile.writeText(gson.toJson(existing))
            plugin.logger.debug("Removed punishment for player $uuid from file.")
        } catch (e: IOException) {
            plugin.logger.severe("Error removing punishment from file: ${e.message}")
        }
    }

    private fun readCacheEntries(): MutableMap<String, CachedPunishment> {
        if (!cacheFile.exists()) return mutableMapOf()

        val json = try {
            cacheFile.readText()
        } catch (e: IOException) {
            plugin.logger.severe("Error reading punishment cache: ${e.message}")
            return mutableMapOf()
        }

        val modernType = object : TypeToken<MutableMap<String, CachedPunishment>>() {}.type
        runCatching { gson.fromJson<MutableMap<String, CachedPunishment>>(json, modernType) }
            .getOrNull()
            ?.let { return it ?: mutableMapOf() }

        val legacyType = object : TypeToken<MutableMap<String, Long>>() {}.type
        val legacy = runCatching { gson.fromJson<MutableMap<String, Long>>(json, legacyType) }.getOrNull()
        if (legacy != null) {
            if (legacy.isNotEmpty()) {
                plugin.logger.debug("Migrating ${legacy.size} legacy jail cache entries to the new format.")
            }
            return legacy.mapValues { CachedPunishment(it.value, null) }.toMutableMap()
        }

        plugin.logger.debug("Punishment cache file is empty or could not be parsed, starting fresh.")
        return mutableMapOf()
    }

    data class CachedPunishment(val endTime: Long, val returnLocation: StoredLocation?)

    data class StoredLocation(
        val world: String,
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Float,
        val pitch: Float
    ) {
        fun toLocation(): Location? {
            val bukkitWorld = Bukkit.getWorld(world) ?: return null
            return Location(bukkitWorld, x, y, z, yaw, pitch)
        }

        companion object {
            fun fromLocation(location: Location): StoredLocation = StoredLocation(
                world = location.world?.name ?: "",
                x = location.x,
                y = location.y,
                z = location.z,
                yaw = location.yaw,
                pitch = location.pitch
            )
        }
    }
}
