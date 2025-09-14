package pl.syntaxdevteam.punisher.api

import pl.syntaxdevteam.punisher.databases.DatabaseHandler
import pl.syntaxdevteam.punisher.databases.PunishmentData
import java.util.concurrent.CompletableFuture

class PunisherXApiImpl(private val databaseHandler: DatabaseHandler) : PunisherXApi {

    override fun getLastTenPunishmentHistory(uuid: String): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            databaseHandler.getPunishmentHistory(uuid, limit = 10)
        }
    }

    override fun getLastTenActivePunishments(uuid: String): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            databaseHandler.getPunishments(uuid, limit = 10)
        }
    }

    override fun getActivePunishments(uuid: String, type: String?): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            val allPunishments = databaseHandler.getPunishments(uuid)
            if (type == null || type.equals("ALL", ignoreCase = true)) {
                allPunishments
            } else {
                allPunishments.filter { it.type.equals(type, ignoreCase = true) }
            }
        }
    }

    override fun getPunishmentHistory(uuid: String, type: String?): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            val allPunishmentHistory = databaseHandler.getPunishmentHistory(uuid)
            if (type == null || type.equals("ALL", ignoreCase = true)) {
                allPunishmentHistory
            } else {
                allPunishmentHistory.filter { it.type.equals(type, ignoreCase = true) }
            }

        }
    }

    override fun getBannedPlayers(limit: Int, offset: Int): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            databaseHandler.getBannedPlayers(limit, offset)
        }
    }

    override fun getHistoryBannedPlayers(limit: Int, offset: Int): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            databaseHandler.getHistoryBannedPlayers(limit, offset)
        }
    }

    override fun getJailedPlayers(limit: Int, offset: Int): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            databaseHandler.getJailedPlayers(limit, offset)
        }
    }
}