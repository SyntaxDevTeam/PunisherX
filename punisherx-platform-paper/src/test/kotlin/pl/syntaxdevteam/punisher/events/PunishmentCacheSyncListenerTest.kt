package pl.syntaxdevteam.punisher.events

import pl.syntaxdevteam.punisher.api.model.PunishmentData
import pl.syntaxdevteam.punisher.core.platform.SchedulerAdapter
import pl.syntaxdevteam.punisher.core.punishment.PunishmentCacheRefresher
import pl.syntaxdevteam.punisher.core.punishment.PunishmentDataCache
import pl.syntaxdevteam.punisher.core.punishment.PunishmentQueryService
import pl.syntaxdevteam.punisher.core.punishment.PunishmentRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class PunishmentCacheSyncListenerTest {

    private val clock = Clock.fixed(Instant.parse("2024-12-01T10:15:30.00Z"), ZoneId.of("UTC"))
    private val repository = FakeRepository()
    private val cache = PunishmentDataCache(clock = clock)
    private val queryService = PunishmentQueryService(repository, cache, clock)
    private val scheduler = ImmediateSchedulerAdapter()
    private val refresher = PunishmentCacheRefresher(queryService, scheduler)
    private val listener = PunishmentCacheSyncListener(refresher)

    @Test
    fun `applied event refreshes cached punishments`() {
        val initial = punishment(1, clock.millis() + 1_000)
        val updated = punishment(2, clock.millis() + 2_000)
        repository.punishments = listOf(initial)
        queryService.getActivePunishments("uuid")

        repository.punishments = listOf(updated)
        listener.onPunishmentApplied(PunishmentAppliedEvent("uuid"))

        assertEquals(listOf(updated), queryService.getActivePunishments("uuid"))
    }

    @Test
    fun `revoked event evicts cached punishments`() {
        val punishment = punishment(3, clock.millis() + 1_000)
        repository.punishments = listOf(punishment)
        queryService.getActivePunishments("uuid")

        repository.punishments = emptyList()
        listener.onPunishmentRevoked(PunishmentRevokedEvent("uuid"))

        assertEquals(emptyList(), queryService.getActivePunishments("uuid"))
    }

    private fun punishment(id: Int, end: Long) = PunishmentData(
        id = id,
        uuid = "uuid",
        type = "BAN",
        reason = "reason",
        start = clock.millis(),
        end = end,
        name = "Steve",
        operator = "Console",
    )

    private class FakeRepository : PunishmentRepository {
        var punishments: List<PunishmentData> = emptyList()

        override fun getPunishments(uuid: String, limit: Int?, offset: Int?): List<PunishmentData> = punishments

        override fun getPunishmentsByIP(ip: String): List<PunishmentData> = emptyList()

        override fun getPunishmentHistory(uuid: String, limit: Int?, offset: Int?): List<PunishmentData> = emptyList()

        override fun getBannedPlayers(limit: Int, offset: Int): List<PunishmentData> = emptyList()

        override fun getHistoryBannedPlayers(limit: Int, offset: Int): List<PunishmentData> = emptyList()

        override fun getJailedPlayers(limit: Int, offset: Int): List<PunishmentData> = emptyList()

        override fun getLastTenPunishments(uuid: String): List<PunishmentData> = punishments.take(10)
    }

    private class ImmediateSchedulerAdapter : SchedulerAdapter {
        override fun runAsync(task: Runnable) { task.run() }
        override fun runSync(task: Runnable) { task.run() }
        override fun runSyncLater(delayTicks: Long, task: Runnable) { task.run() }
        override fun runRegionally(anchor: Any, task: Runnable) { task.run() }
    }
}
