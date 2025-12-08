package pl.syntaxdevteam.punisher.api

import pl.syntaxdevteam.punisher.api.model.PunishmentData
import pl.syntaxdevteam.punisher.core.punishment.PunishmentRepository
import java.util.concurrent.CompletableFuture

class PunisherXApiImpl(private val punishmentRepository: PunishmentRepository) : PunisherXApi {

    override fun getLastTenPunishmentHistory(uuid: String): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            punishmentRepository.getPunishmentHistory(uuid, limit = 10)
        }
    }

    override fun getLastTenActivePunishments(uuid: String): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            punishmentRepository.getPunishments(uuid, limit = 10)
        }
    }

    override fun getActivePunishments(uuid: String, type: String?): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            val allPunishments = punishmentRepository.getPunishments(uuid)
            if (type == null || type.equals("ALL", ignoreCase = true)) {
                allPunishments
            } else {
                allPunishments.filter { it.type.equals(type, ignoreCase = true) }
            }
        }
    }

    override fun getPunishmentHistory(uuid: String, type: String?): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            val allPunishmentHistory = punishmentRepository.getPunishmentHistory(uuid)
            if (type == null || type.equals("ALL", ignoreCase = true)) {
                allPunishmentHistory
            } else {
                allPunishmentHistory.filter { it.type.equals(type, ignoreCase = true) }
            }

        }
    }

    override fun getBannedPlayers(limit: Int, offset: Int): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            punishmentRepository.getBannedPlayers(limit, offset)
        }
    }

    override fun getHistoryBannedPlayers(limit: Int, offset: Int): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            punishmentRepository.getHistoryBannedPlayers(limit, offset)
        }
    }

    override fun getJailedPlayers(limit: Int, offset: Int): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            punishmentRepository.getJailedPlayers(limit, offset)
        }
    }

    override fun isMuted(uuid: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            punishmentRepository.getPunishments(uuid).any { it.type.equals("MUTE", ignoreCase = true) }
        }
    }

    override fun isJailed(uuid: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            punishmentRepository.getPunishments(uuid).any { it.type.equals("JAIL", ignoreCase = true) }
    }
}
}