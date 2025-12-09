package pl.syntaxdevteam.punisher.core.punishment

import pl.syntaxdevteam.punisher.api.model.PunishmentData
import java.time.Clock

/**
 * Warstwa zapytań i filtracji kar działająca w core, oparta na [PunishmentRepository]
 * i wspólnym cache Caffeine. Umożliwia platformom podpinanie własnych repozytoriów
 * bez duplikowania logiki filtrowania i określania stanu kary.
 */
class PunishmentQueryService(
    private val repository: PunishmentRepository,
    private val cache: PunishmentDataCache,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun getActivePunishments(uuid: String, type: String? = null, limit: Int? = null, offset: Int? = null): List<PunishmentData> {
        val active = cache.getActivePunishments(uuid)
            ?: repository.getPunishments(uuid, limit, offset).also { cache.cacheActivePunishments(uuid, it) }

        return active
            .filter(::isActive)
            .filterByType(type)
    }

    fun getPunishmentHistory(uuid: String, type: String? = null, limit: Int? = null, offset: Int? = null): List<PunishmentData> {
        return repository.getPunishmentHistory(uuid, limit, offset).filterByType(type)
    }

    fun getBannedPlayers(limit: Int, offset: Int): List<PunishmentData> =
        repository.getBannedPlayers(limit, offset)

    fun getHistoryBannedPlayers(limit: Int, offset: Int): List<PunishmentData> =
        repository.getHistoryBannedPlayers(limit, offset)

    fun getJailedPlayers(limit: Int, offset: Int): List<PunishmentData> =
        repository.getJailedPlayers(limit, offset)

    fun getLastTenPunishmentHistory(uuid: String): List<PunishmentData> =
        repository.getPunishmentHistory(uuid, limit = 10)

    fun getLastTenActivePunishments(uuid: String): List<PunishmentData> =
        getActivePunishments(uuid, limit = 10)

    fun isMuted(uuid: String): Boolean =
        getActivePunishments(uuid, type = "MUTE").isNotEmpty()

    fun isJailed(uuid: String): Boolean =
        getActivePunishments(uuid, type = "JAIL").isNotEmpty()

    fun invalidate(uuid: String) {
        cache.invalidate(uuid)
    }

    private fun List<PunishmentData>.filterByType(type: String?): List<PunishmentData> {
        if (type == null || type.equals("ALL", ignoreCase = true)) return this
        return filter { it.type.equals(type, ignoreCase = true) }
    }

    private fun isActive(punishment: PunishmentData): Boolean {
        val now = clock.millis()
        return punishment.end == -1L || punishment.end > now
    }
}
