package pl.syntaxdevteam.punisher.core.punishment

import pl.syntaxdevteam.punisher.api.model.PunishmentData

/**
 * Minimal data access contract needed by punishment API and core services.
 */
interface PunishmentRepository {
    fun getPunishments(uuid: String, limit: Int? = null, offset: Int? = null): List<PunishmentData>
    fun getPunishmentsByIP(ip: String): List<PunishmentData>
    fun getPunishmentHistory(uuid: String, limit: Int? = null, offset: Int? = null): List<PunishmentData>
    fun getBannedPlayers(limit: Int, offset: Int): List<PunishmentData>
    fun getHistoryBannedPlayers(limit: Int, offset: Int): List<PunishmentData>
    fun getJailedPlayers(limit: Int, offset: Int): List<PunishmentData>
    fun getLastTenPunishments(uuid: String): List<PunishmentData>
}
