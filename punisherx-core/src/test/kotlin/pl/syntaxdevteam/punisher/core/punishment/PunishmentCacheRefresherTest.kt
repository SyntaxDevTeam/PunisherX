package pl.syntaxdevteam.punisher.core.punishment

import pl.syntaxdevteam.punisher.api.model.PunishmentData
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class PunishmentCacheRefresherTest {

    private val clock = Clock.fixed(Instant.parse("2024-12-01T10:15:30.00Z"), ZoneId.of("UTC"))
    private val repository = FakeRepository()
    private val cache = PunishmentDataCache(clock = clock)
    private val queryService = PunishmentQueryService(repository, cache, clock)
    private val scheduler = ImmediateSchedulerAdapter()
    private val refresher = PunishmentCacheRefresher(queryService, scheduler)

    @Test
    fun `refreshAsync invalidates stale cache and preloads new state`() {
        val initial = punishment(id = 1, end = clock.millis() + 1_000)
        val updated = punishment(id = 2, end = clock.millis() + 2_000)
        repository.punishments = listOf(initial)

        // warm cache
        assertEquals(listOf(initial), queryService.getActivePunishments("uuid"))

        repository.punishments = listOf(updated)
        refresher.refreshAsync("uuid")

        assertEquals(listOf(updated), queryService.getActivePunishments("uuid"))
    }

    @Test
    fun `evictAsync clears cache without preloading`() {
        val punishment = punishment(id = 3, end = clock.millis() + 1_000)
        repository.punishments = listOf(punishment)
        queryService.getActivePunishments("uuid") // warm cache

        repository.punishments = emptyList()
        refresher.evictAsync("uuid")

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

    private class ImmediateSchedulerAdapter : pl.syntaxdevteam.punisher.core.platform.SchedulerAdapter {
        override fun runAsync(task: Runnable) { task.run() }
        override fun runSync(task: Runnable) { task.run() }
        override fun runSyncLater(delayTicks: Long, task: Runnable) { task.run() }
        override fun runRegionally(anchor: Any, task: Runnable) { task.run() }
    }
}
