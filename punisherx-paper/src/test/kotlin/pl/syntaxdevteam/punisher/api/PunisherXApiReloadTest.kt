package pl.syntaxdevteam.punisher.api

import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import pl.syntaxdevteam.punisher.databases.DatabaseHandler
import pl.syntaxdevteam.punisher.databases.PunishmentData
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class PunisherXApiReloadTest {
    private val uuid = "84002a42-c75f-3b0d-b2e0-dc1b395d5bdc"
    private val mute = PunishmentData(1, uuid, "MUTE", "test", 0, -1, "player", "console", "network")

    @Test
    fun `API retained by another plugin uses replacement database after reload`() {
        val original = mock<DatabaseHandler>()
        val replacement = mock<DatabaseHandler>()
        whenever(original.isReady()).thenReturn(true)
        whenever(replacement.isReady()).thenReturn(true)
        whenever(original.getPunishments(uuid)).thenReturn(emptyList())
        whenever(replacement.getPunishments(uuid)).thenReturn(listOf(mute))
        var current = original
        val retainedApi = PunisherXApiImpl { current }

        assertEquals(emptyList(), retainedApi.getActivePunishments(uuid, null).get(5, TimeUnit.SECONDS))
        current = replacement
        assertEquals(listOf(mute), retainedApi.getActivePunishments(uuid, "MUTE").get(5, TimeUnit.SECONDS))
        assertEquals(true, retainedApi.isMuted(uuid).get(5, TimeUnit.SECONDS))
        verify(original).isReady()
        verify(original).getPunishments(uuid)
        verifyNoMoreInteractions(original)
    }

    @Test
    fun `API fails explicitly during database setup and recovers when ready`() {
        val database = mock<DatabaseHandler>()
        whenever(database.isReady()).thenReturn(false)
        val api = PunisherXApiImpl { database }

        val failure = assertFailsWith<ExecutionException> {
            api.getActivePunishments(uuid, null).get(5, TimeUnit.SECONDS)
        }
        assertIs<IllegalStateException>(failure.cause)
        verify(database).isReady()
        verifyNoMoreInteractions(database)

        whenever(database.isReady()).thenReturn(true)
        whenever(database.getPunishments(uuid)).thenReturn(listOf(mute))
        assertEquals(listOf(mute), api.getActivePunishments(uuid, null).get(5, TimeUnit.SECONDS))
    }
}
