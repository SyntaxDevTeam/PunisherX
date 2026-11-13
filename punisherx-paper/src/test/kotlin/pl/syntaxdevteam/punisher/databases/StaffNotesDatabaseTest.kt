package pl.syntaxdevteam.punisher.databases

import org.bukkit.configuration.file.YamlConfiguration
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import pl.syntaxdevteam.punisher.PunisherX
import java.nio.file.Files
import java.util.UUID
import kotlin.test.*

class StaffNotesDatabaseTest {
    private val folder = Files.createTempDirectory("punisher-notes-").toFile()
    private val config = YamlConfiguration().apply {
        set("database.type", "sqlite")
        set("database.sql.dbname", "notes.sqlite")
        set("server", "survival")
    }
    private val plugin = mock<PunisherX>().also {
        whenever(it.config).thenReturn(config)
        whenever(it.dataFolder).thenReturn(folder)
        whenever(it.name).thenReturn("PunisherX")
        whenever(it.logger).thenReturn(mock())
    }
    private val handler = DatabaseHandler(plugin)

    @AfterTest fun cleanup() {
        handler.closeConnection()
        folder.deleteRecursively()
    }

    @Test fun `notes are isolated by target and pagination`() {
        handler.openConnection()
        handler.createTables()
        val alice = UUID.randomUUID()
        val bob = UUID.randomUUID()
        val first = assertNotNull(handler.addStaffNote(alice, "Moderator", "Suspicious movement"))
        val second = assertNotNull(handler.addStaffNote(alice, "Admin", "Needs review"))
        assertEquals(listOf(second, first), handler.getStaffNotes(alice).map { it.id })
        assertEquals(listOf(first), handler.getStaffNotes(alice, 1, 1).map { it.id })
        assertEquals("survival", handler.getStaffNotes(alice).first().server)
        assertTrue(handler.getStaffNotes(bob).isEmpty())
        assertFalse(handler.removeStaffNote(bob, first))
        assertTrue(handler.removeStaffNote(alice, first))
        assertFalse(handler.removeStaffNote(alice, first))
    }

    @Test fun `invalid notes do not persist`() {
        handler.openConnection()
        handler.createTables()
        val player = UUID.randomUUID()
        assertNull(handler.addStaffNote(player, "Admin", "x"))
        assertNull(handler.addStaffNote(player, "Admin", "line\nnext"))
        assertNull(handler.addStaffNote(player, "Admin", "x".repeat(501)))
        assertTrue(handler.getStaffNotes(player).isEmpty())
    }
}
