package pl.syntaxdevteam.punisher.api

import pl.syntaxdevteam.punisher.databases.DatabaseHandler
import pl.syntaxdevteam.punisher.databases.PunishmentData
import java.util.concurrent.CompletableFuture

class PunisherXApiImpl(private val databaseHandler: DatabaseHandler) : PunisherXApi {

    override fun getLastTenPunishmentHistory(uuid: String): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            databaseHandler.getPunishmentHistory(uuid, 10, 0)
        }
    }

    override fun getLastTenActivePunishments(uuid: String): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            databaseHandler.getLastTenPunishments(uuid)
        }
    }

    override fun getActivePunishments(type: String?): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            val filterType = type ?: "ALL"
            databaseHandler.getPunishments(filterType)
        }
    }

    override fun getPunishmentHistory(type: String?): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            val filterType = type ?: "ALL"
            databaseHandler.getPunishments(filterType)
        }
    }

    override fun getActivePunishmentsCount(uuid: String, type: String): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            databaseHandler.getActiveWarnCount(uuid)
        }
    }
}
