package pl.syntaxdevteam.punisher.core.punishment

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import pl.syntaxdevteam.punisher.api.model.PunishmentData
import java.time.Clock
import java.time.Duration

/**
 * Cache przechowujący aktywne kary w pamięci, aby ograniczyć liczbę zapytań
 * do warstwy danych. Zgodnie z planem 2.0 całość logiki zostaje przeniesiona
 * do core i korzysta z Caffeine jako jedynej biblioteki cache.
 */
class PunishmentDataCache(
    expireAfterAccess: Duration = Duration.ofMinutes(10),
    maximumSize: Long = 10_000,
    private val clock: Clock = Clock.systemUTC(),
) {

    private val cache: Cache<String, List<PunishmentData>> = Caffeine.newBuilder()
        .expireAfterAccess(expireAfterAccess)
        .maximumSize(maximumSize)
        .build()

    fun cacheActivePunishments(uuid: String, punishments: List<PunishmentData>) {
        cache.put(uuid, punishments.filter(::isActive))
    }

    fun getActivePunishments(uuid: String): List<PunishmentData>? =
        cache.getIfPresent(uuid)?.filter(::isActive)

    fun invalidate(uuid: String) {
        cache.invalidate(uuid)
    }

    fun invalidateAll() {
        cache.invalidateAll()
    }

    private fun isActive(punishment: PunishmentData): Boolean {
        val now = clock.millis()
        return punishment.end == -1L || punishment.end > now
    }
}
