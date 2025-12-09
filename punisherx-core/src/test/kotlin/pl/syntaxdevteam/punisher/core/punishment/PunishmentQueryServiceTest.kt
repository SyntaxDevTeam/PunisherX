package pl.syntaxdevteam.punisher.core.punishment

import pl.syntaxdevteam.punisher.api.model.PunishmentData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class PunishmentQueryServiceTest {

    private val clock = Clock.fixed(Instant.parse("2024-12-01T10:15:30.00Z"), ZoneId.of("UTC"))
    private val repository = FakeRepository(clock)
    private val cache = PunishmentDataCache(clock = clock)
    private val service = PunishmentQueryService(repository, cache, clock)

    @Test
    fun `uses cache for active punishments after first retrieval`() {
        val active = punishment(id = 1, type = "BAN", end = clock.millis() + 1_000)
        val expired = punishment(id = 2, type = "MUTE", end = clock.millis() - 1)
        repository.punishments = listOf(active, expired)

        val firstCall = service.getActivePunishments("uuid")
        repository.punishments = emptyList()
        val secondCall = service.getActivePunishments("uuid")

        assertEquals(listOf(active), firstCall)
        assertEquals(listOf(active), secondCall)
        assertEquals(1, repository.punishmentCalls)
    }

    @Test
    fun `filters by type ignoring case`() {
        val mute = punishment(id = 3, type = "MUTE", end = -1)
        val jail = punishment(id = 4, type = "JAIL", end = -1)
        repository.punishments = listOf(mute, jail)

        val result = service.getActivePunishments("uuid", type = "mute")

        assertEquals(listOf(mute), result)
    }

    @Test
    fun `reports mute and jail status based on active punishments`() {
        val jail = punishment(id = 5, type = "JAIL", end = clock.millis() + 5_000)
        repository.punishments = listOf(jail)

        assertTrue(service.isJailed("uuid"))
        assertFalse(service.isMuted("uuid"))
    }

    @Test
    fun `delegates history and list queries to repository`() {
        val history = punishment(id = 6, type = "BAN", end = -1)
        val banned = punishment(id = 7, type = "BAN", end = -1)
        val jailed = punishment(id = 8, type = "JAIL", end = -1)
        repository.history = listOf(history)
        repository.banned = listOf(banned)
        repository.historyBanned = listOf(history)
        repository.jailed = listOf(jailed)

        assertEquals(listOf(history), service.getPunishmentHistory("uuid"))
        assertEquals(listOf(banned), service.getBannedPlayers(10, 0))
        assertEquals(listOf(history), service.getHistoryBannedPlayers(10, 0))
        assertEquals(listOf(jailed), service.getJailedPlayers(10, 0))
    }

    private fun punishment(id: Int, type: String, end: Long) = PunishmentData(
        id = id,
        uuid = "uuid",
        type = type,
        reason = "reason",
        start = clock.millis(),
        end = end,
        name = "Steve",
        operator = "Console",
    )

    private class FakeRepository(private val clock: Clock) : PunishmentRepository {
        var punishments: List<PunishmentData> = emptyList()
        var history: List<PunishmentData> = emptyList()
        var banned: List<PunishmentData> = emptyList()
        var historyBanned: List<PunishmentData> = emptyList()
        var jailed: List<PunishmentData> = emptyList()
        var punishmentCalls: Int = 0

        override fun getPunishments(uuid: String, limit: Int?, offset: Int?): List<PunishmentData> {
            punishmentCalls++
            return punishments
        }

        override fun getPunishmentsByIP(ip: String): List<PunishmentData> = emptyList()

        override fun getPunishmentHistory(uuid: String, limit: Int?, offset: Int?): List<PunishmentData> = history

        override fun getBannedPlayers(limit: Int, offset: Int): List<PunishmentData> = banned

        override fun getHistoryBannedPlayers(limit: Int, offset: Int): List<PunishmentData> = historyBanned

        override fun getJailedPlayers(limit: Int, offset: Int): List<PunishmentData> = jailed

        override fun getLastTenPunishments(uuid: String): List<PunishmentData> = punishments.take(10)
    }
}
