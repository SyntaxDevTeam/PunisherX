package pl.syntaxdevteam.punisher.stats

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.bukkit.Bukkit

object PlayerStatsService {
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = mutableMapOf<UUID, Cached>()

    private data class Cached(
        val json: JsonObject,
        val lastModified: Long
    )

    /** Async preload + cache z prostą inwalidacją po mtime pliku */
    fun preloadAsync(uuid: UUID): Deferred<JsonObject?> = scope.async {
        val f = File("world/stats/$uuid.json")
        if (!f.exists()) return@async null

        val lm = f.lastModified()
        val cached = cache[uuid]
        if (cached != null && cached.lastModified == lm) return@async cached.json

        val json = gson.fromJson(f.readText(), JsonObject::class.java)
        cache[uuid] = Cached(json, lm)
        json
    }

    /** Bezpieczne pobranie z cache (jeśli brak, zaczyta sync na IO) */
    suspend fun getJson(uuid: UUID): JsonObject? =
        preloadAsync(uuid).await()

    // ========== HELPERY WYCIĄGAJĄCE KONKRETNE DANE ==========

    /** 1) Łączny czas gry jako gotowy String (hh:mm:ss). Źródło: stats JSON (ticki). */
    suspend fun getTotalPlaytimeString(uuid: UUID): String? {
        val json = getJson(uuid) ?: return null
        val stats = json.getAsJsonObject("stats") ?: return null
        val custom = stats.getAsJsonObject("minecraft:custom") ?: return null

        val ticks = when {
            custom.has("minecraft:play_time") -> custom.get("minecraft:play_time").asLong
            custom.has("minecraft:play_one_minute") -> custom.get("minecraft:play_one_minute").asLong // starsze światy
            else -> return null
        }
        return formatDuration(Duration.ofSeconds(ticks / 20))
    }

    /** 2) Aktualny czas online w bieżącej sesji (jeśli gracz online), lub ostatni czas online (czas trwania poprzedniej sesji, jeśli chcesz – patrz komentarz). */
    fun getCurrentOnlineString(uuid: UUID): String? {
        val off = Bukkit.getOfflinePlayer(uuid)
        // Paper API: lastLogin/lastSeen → pewne źródła czasu
        val now = System.currentTimeMillis()
        // Jeśli online – czas sesji = now - lastLogin
        return if (off.isOnline) {
            val start = off.lastLogin
            if (start <= 0) return null
            formatDuration(Duration.ofMillis(now - start))
        } else {
            // Offline – jeśli chcesz "ostatni czas online", potrzebujesz znanego czasu logowania i wylogowania.
            // lastSeen = moment wylogowania; lastLogin = moment zalogowania.
            val login = off.lastLogin
            val seen = off.lastSeen
            if (login > 0 && seen > 0 && seen >= login) {
                formatDuration(Duration.ofMillis(seen - login))
            } else null
        }
    }

    /** 3a) Ilość wejść na serwer – Minecraft NIE prowadzi takiej statystyki. Trzeba liczyć samemu. */
    // Rekomendacja niżej: zliczaj w PlayerJoinEvent i zapisuj w DB/konfigu.

    /** 3b) Data ostatniego wejścia / ostatnio widziany */
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

    private fun formatDuration(d: Duration): String {
        val h = d.toHours()
        val m = d.toMinutesPart()
        val s = d.toSecondsPart()
        return "%dh %02dm %02ds".format(h, m, s)
    }
}
