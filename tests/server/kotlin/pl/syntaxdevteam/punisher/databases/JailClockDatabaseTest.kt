package pl.syntaxdevteam.punisher.databases

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.mock
import kotlin.test.*

class JailClockDatabaseTest : SyntaxDbTestFixture() {
    private fun deadline(id: Int): Long = jdbc { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT endTime FROM punishments WHERE id = $id").use { it.next(); it.getLong(1) } }
    }
    @Test fun `online sentence pauses persists and resumes without changing mute`() {
        config.set("jail.online_time_only", true)
        start()
        mockStatic(Bukkit::class.java).use { bukkit ->
            bukkit.`when`<Player?> { Bukkit.getPlayer(uuid) }.thenReturn(mock<Player>())
            val end = System.currentTimeMillis() + 600000
            val id = add(type = "JAIL", end = end)
            val muteId = add(type = "MUTE", end = end)
            handler.updateJailClock(uuid.toString(), true)
            val paused = deadline(id)
            val restored = DatabaseHandler(plugin)
            restored.openConnection()
            try {
            assertTrue(paused < -1)
            restored.updateJailClock(uuid.toString(), true)
            assertEquals(paused, deadline(id))
            assertEquals(end, deadline(muteId))
            restored.updateJailClock(uuid.toString(), false)
            assertTrue(deadline(id) >= end)
            assertEquals(end, deadline(muteId))
            } finally { restored.closeConnection() }
        }
    }
    @Test fun `offline sentence starts paused and permanent sentence remains permanent`() {
        config.set("jail.online_time_only", true)
        start()
        mockStatic(Bukkit::class.java).use {
            val id = add(type = "JAIL", end = System.currentTimeMillis() + 600000)
            assertTrue(deadline(id) < -1)
            assertTrue(handler.getPunishments(uuid.toString()).single().end > System.currentTimeMillis())
            val permanent = add(type = "JAIL", end = -1)
            handler.updateJailClock(uuid.toString(), true)
            handler.updateJailClock(uuid.toString(), false)
            assertEquals(-1, deadline(permanent))
        }
    }
    @Test fun `wall clock mode leaves deadline unchanged on logout`() {
        start()
        val end = System.currentTimeMillis() + 600000
        val id = add(type = "JAIL", end = end)
        handler.updateJailClock(uuid.toString(), true)
        assertEquals(end, deadline(id))
    }
}
