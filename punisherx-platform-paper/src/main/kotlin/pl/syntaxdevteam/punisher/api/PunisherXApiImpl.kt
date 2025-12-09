package pl.syntaxdevteam.punisher.api

import pl.syntaxdevteam.punisher.api.model.PunishmentData
import pl.syntaxdevteam.punisher.core.punishment.PunishmentQueryService
import java.util.concurrent.CompletableFuture

class PunisherXApiImpl(private val queryService: PunishmentQueryService) : PunisherXApi {

    override fun getLastTenPunishmentHistory(uuid: String): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            queryService.getLastTenPunishmentHistory(uuid)
        }
    }

    override fun getLastTenActivePunishments(uuid: String): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            queryService.getLastTenActivePunishments(uuid)
        }
    }

    override fun getActivePunishments(uuid: String, type: String?): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            queryService.getActivePunishments(uuid, type)
        }
    }

    override fun getPunishmentHistory(uuid: String, type: String?): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            queryService.getPunishmentHistory(uuid, type)
        }
    }

    override fun getBannedPlayers(limit: Int, offset: Int): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            queryService.getBannedPlayers(limit, offset)
        }
    }

    override fun getHistoryBannedPlayers(limit: Int, offset: Int): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            queryService.getHistoryBannedPlayers(limit, offset)
        }
    }

    override fun getJailedPlayers(limit: Int, offset: Int): CompletableFuture<List<PunishmentData>> {
        return CompletableFuture.supplyAsync {
            queryService.getJailedPlayers(limit, offset)
        }
    }

    override fun isMuted(uuid: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            queryService.isMuted(uuid)
        }
    }

    override fun isJailed(uuid: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            queryService.isJailed(uuid)
        }
    }
}