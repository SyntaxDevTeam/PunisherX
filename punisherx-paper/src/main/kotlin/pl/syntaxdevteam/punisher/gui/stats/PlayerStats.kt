package pl.syntaxdevteam.punisher.gui.stats

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import net.kyori.adventure.nbt.BinaryTagIO
import net.kyori.adventure.nbt.BinaryTagTypes
import org.bukkit.Location

/**
 * Provides access to basic player statistics stored in the vanilla stats JSON files
 * located under the world folder. Results are cached based on the file's last
 * modification time so repeated lookups are cheap.
 */
object PlayerStatsService {
    private val gson = Gson()
    private val cache = java.util.concurrent.ConcurrentHashMap<UUID, Cached>()
    private val logoutLocations = java.util.concurrent.ConcurrentHashMap<UUID, LogoutLocation>()
    private var locationFolder: File? = null

    private data class LogoutLocation(val world: String, val x: Double, val y: Double, val z: Double)

    fun initialize(dataFolder: File) {
        locationFolder = File(dataFolder, "logout-locations")
        logoutLocations.clear()
    }

    /** Capture on the player's thread, then persist the immutable snapshot asynchronously. */
    fun captureLogoutLocation(uuid: UUID, location: Location): Runnable {
        val snapshot = LogoutLocation(location.world!!.name, location.x, location.y, location.z)
        logoutLocations[uuid] = snapshot
        val folder = locationFolder
        return Runnable {
            if (folder != null) {
                try {
                    folder.mkdirs()
                    synchronized(logoutLocations) {
                        if (logoutLocations[uuid] === snapshot) {
                            val destination = File(folder, "$uuid.json")
                            val temporary = File(folder, "$uuid.json.tmp")
                            temporary.writeText(gson.toJson(snapshot))
                            java.nio.file.Files.move(
                                temporary.toPath(), destination.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING
                            )
                        }
                    }
                } catch (ex: Exception) {
                    Bukkit.getLogger().warning("Failed to save logout location for $uuid: $ex")
                }
            }
        }
    }

    private fun loadLogoutLocation(uuid: UUID): LogoutLocation? {
        logoutLocations[uuid]?.let { return it }
        val folder = locationFolder ?: return null
        val file = File(folder, "$uuid.json")
        if (!file.exists()) return null
        return runCatching { gson.fromJson(file.readText(), LogoutLocation::class.java) }.getOrNull()
    }

    private data class Cached(
        val json: JsonObject,
        val lastModified: Long
    )

    private fun loadJson(uuid: UUID): JsonObject? {
        val f = File("world/stats/$uuid.json")
        if (!f.exists()) return null

        val lm = f.lastModified()
        val cached = cache[uuid]
        if (cached != null && cached.lastModified == lm) return cached.json

        val json = gson.fromJson(f.readText(), JsonObject::class.java)
        cache[uuid] = Cached(json, lm)
        return json
    }

    /**
     * Total playtime across all sessions formatted as "Xh Ym Zs".
     */
    fun getTotalPlaytimeString(uuid: UUID): String? {
        val json = loadJson(uuid) ?: return null
        val stats = json.getAsJsonObject("stats") ?: return null
        val custom = stats.getAsJsonObject("minecraft:custom") ?: return null

        val ticks = when {
            custom.has("minecraft:play_time") -> custom.get("minecraft:play_time").asLong
            custom.has("minecraft:play_one_minute") -> custom.get("minecraft:play_one_minute").asLong // legacy worlds
            else -> return null
        }
        return formatDuration(Duration.ofSeconds(ticks / 20))
    }

    /**
     * Current session playtime for online players, or the length of the previous
     * session for offline players when possible.
     */
    fun getCurrentOnlineString(uuid: UUID): String? {
        val off = Bukkit.getOfflinePlayer(uuid)
        val now = System.currentTimeMillis()

        return if (off.isOnline) {
            val start = off.lastLogin
            if (start <= 0) return null
            formatDuration(Duration.ofMillis(now - start))
        } else {
            val login = off.lastLogin
            val seen = off.lastSeen
            if (login > 0 && seen > 0 && seen >= login) {
                formatDuration(Duration.ofMillis(seen - login))
            } else null
        }
    }
    //TODO: sprawdzić dlaczego nie są używane te metody i czy w ogóle są potrzebne.
    fun getLastLoginDate(uuid: UUID, zone: ZoneId = ZoneId.systemDefault()): String? {
        val off = Bukkit.getOfflinePlayer(uuid)
        val ts = off.lastLogin
        if (ts <= 0) return null
        return Instant.ofEpochMilli(ts).atZone(zone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }

    fun getLastSeenDate(uuid: UUID, zone: ZoneId = ZoneId.systemDefault()): String? {
        val off = Bukkit.getOfflinePlayer(uuid)
        val ts = off.lastSeen
        if (ts <= 0) return null
        return Instant.ofEpochMilli(ts).atZone(zone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }

    fun getLastActiveString(uuid: UUID, zone: ZoneId = ZoneId.systemDefault()): String? {
        val off = Bukkit.getOfflinePlayer(uuid)
        val ts = if (off.isOnline) off.lastLogin else off.lastSeen
        if (ts <= 0) return null
        return Instant.ofEpochMilli(ts).atZone(zone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
    //TODO: Zredukować kod dla getLastLocation i getLastLocationString do jednej metody a następnie poprawić logikę w GUI w miejscu użycia tych metod.
    @Suppress("UnstableApiUsage")
    fun getLastLocationString(uuid: UUID): String? {
        loadLogoutLocation(uuid)?.let { snapshot ->
            return "${snapshot.world}: ${kotlin.math.floor(snapshot.x).toInt()}, ${kotlin.math.floor(snapshot.y).toInt()}, ${kotlin.math.floor(snapshot.z).toInt()}"
        }
        return try {
            val worldFolder = Bukkit.getWorlds().firstOrNull()?.worldFolder ?: return null
            val dataFile = File(worldFolder, "playerdata/$uuid.dat")
            if (!dataFile.exists()) return null

            val tag = BinaryTagIO.reader()
                .read(dataFile.toPath(), BinaryTagIO.Compression.GZIP)

            val pos = tag.getList("Pos", BinaryTagTypes.DOUBLE)
            val x = pos.getDouble(0)
            val y = pos.getDouble(1)
            val z = pos.getDouble(2)

            val worldKey = NamespacedKey.fromString(tag.getString("Dimension"))
            val worldName = worldKey?.let { Bukkit.getWorld(it)?.name } ?: tag.getString("Dimension")

            "$worldName: ${x.toInt()}, ${y.toInt()}, ${z.toInt()}"
        } catch (ex: Exception) {
            Bukkit.getLogger().fine("Failed to load last location for $uuid: $ex")
            null
        }
    }

    fun getLastLocation(uuid: UUID): Location? {
        loadLogoutLocation(uuid)?.let { snapshot ->
            val world = Bukkit.getWorld(snapshot.world) ?: return null
            return Location(world, snapshot.x, snapshot.y, snapshot.z)
        }
        return try {
            val worldFolder = Bukkit.getWorlds().firstOrNull()?.worldFolder ?: return null
            val dataFile = File(worldFolder, "playerdata/$uuid.dat")
            if (!dataFile.exists()) return null

            val tag = BinaryTagIO.reader()
                .read(dataFile.toPath(), BinaryTagIO.Compression.GZIP)

            val pos = tag.getList("Pos", BinaryTagTypes.DOUBLE)
            val x = pos.getDouble(0)
            val y = pos.getDouble(1)
            val z = pos.getDouble(2)

            val worldKey = NamespacedKey.fromString(tag.getString("Dimension"))
            val world = worldKey?.let { Bukkit.getWorld(it) } ?: Bukkit.getWorld(tag.getString("Dimension"))

            world?.let { Location(it, x, y, z) }
        } catch (ex: Exception) {
            Bukkit.getLogger().fine("Failed to load last location for $uuid: $ex")
            null
        }
    }


    private fun formatDuration(d: Duration): String {
        val h = d.toHours()
        val m = d.toMinutesPart()
        val s = d.toSecondsPart()
        return "%dh %02dm %02ds".format(h, m, s)
    }
}
