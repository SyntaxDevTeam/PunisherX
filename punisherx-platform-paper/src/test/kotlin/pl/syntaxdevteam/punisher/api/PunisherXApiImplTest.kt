package pl.syntaxdevteam.punisher.api

import pl.syntaxdevteam.punisher.api.model.PunishmentData
import pl.syntaxdevteam.punisher.core.punishment.PunishmentRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PunisherXApiImplTest {

    private val repository = FakePunishmentRepository()
    private val api = PunisherXApiImpl(repository)

    @Test
    fun `filters active punishments by type`() {
        repository.punishments = listOf(
            punishment(id = 1, type = "MUTE"),
            punishment(id = 2, type = "BAN"),
        )

        val result = api.getActivePunishments("uuid", "mute").get()

        assertEquals(listOf(repository.punishments.first()), result)
    }

    @Test
    fun `filters punishment history by type`() {
        repository.history = listOf(
            punishment(id = 3, type = "JAIL"),
            punishment(id = 4, type = "MUTE"),
        )

        val result = api.getPunishmentHistory("uuid", "jail").get()

        assertEquals(listOf(repository.history.first()), result)
    }

    @Test
    fun `reports mute and jail status`() {
        repository.punishments = listOf(punishment(id = 5, type = "JAIL"))

        assertTrue(api.isJailed("uuid").get())
        assertFalse(api.isMuted("uuid").get())
    }

    @Test
    fun `returns future results directly for list endpoints`() {
        repository.banned = listOf(punishment(id = 6, type = "BAN"))
        repository.historyBanned = listOf(punishment(id = 7, type = "BAN"))
        repository.jailed = listOf(punishment(id = 8, type = "JAIL"))

        assertEquals(1, api.getBannedPlayers(10, 0).get().size)
        assertEquals(1, api.getHistoryBannedPlayers(10, 0).get().size)
        assertEquals(1, api.getJailedPlayers(10, 0).get().size)
    }

    private fun punishment(id: Int, type: String) = PunishmentData(
        id = id,
        uuid = "uuid",
        type = type,
        reason = "reason",
        start = 0,
        end = 0,
        name = "Steve",
        operator = "Console",
    )

    private class FakePunishmentRepository : PunishmentRepository {
        var punishments: List<PunishmentData> = emptyList()
        var history: List<PunishmentData> = emptyList()
        var banned: List<PunishmentData> = emptyList()
        var historyBanned: List<PunishmentData> = emptyList()
        var jailed: List<PunishmentData> = emptyList()

        override fun getPunishments(uuid: String, limit: Int?, offset: Int?) = punishments

        override fun getPunishmentsByIP(ip: String): List<PunishmentData> = emptyList()

        override fun getPunishmentHistory(uuid: String, limit: Int?, offset: Int?) = history

        override fun getBannedPlayers(limit: Int, offset: Int) = banned

        override fun getHistoryBannedPlayers(limit: Int, offset: Int) = historyBanned

        override fun getJailedPlayers(limit: Int, offset: Int) = jailed

        override fun getLastTenPunishments(uuid: String) = punishments.take(10)
    }
}
