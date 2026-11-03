package pl.syntaxdevteam.punisher.gui

import net.kyori.adventure.text.Component
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.compatibility.platform.SchedulerAdapter
import pl.syntaxdevteam.punisher.databases.DatabaseHandler
import java.nio.file.Files
import kotlin.test.*

class GuiPunishmentUndoTest {
    @Test fun `panel undo removes first mute from escalation and preserves later punishments`() {
        val folder = Files.createTempDirectory("gui-undo").toFile()
        val plugin = mock<PunisherX>()
        val operator = mock<Player>()
        val scheduler = mock<SchedulerAdapter>()
        whenever(operator.name).thenReturn("Reviewer")
        whenever(operator.hasPermission(any<String>())).thenReturn(true)
        whenever(plugin.config).thenReturn(YamlConfiguration().apply {
            set("database.type", "sqlite")
            set("database.sql.dbname", "test.sqlite")
            set("server", "survival")
        })
        whenever(plugin.dataFolder).thenReturn(folder)
        whenever(plugin.logger).thenReturn(mock())
        whenever(plugin.guiRuntime).thenReturn(GuiRuntime(plugin))
        whenever(plugin.discordWebhook).thenReturn(mock())
        whenever(plugin.punishmentManager).thenReturn(pl.syntaxdevteam.punisher.basic.PunishmentManager())
        whenever(plugin.messageHandler).thenReturn(mock())
        whenever(plugin.messageHandler.miniMessageFormat(any())).thenReturn(Component.text("Removed"))
        whenever(plugin.schedulerAdapter).thenReturn(scheduler)
        doAnswer { it.getArgument<Runnable>(0).run(); null }.whenever(scheduler).runAsync(any())
        doAnswer { it.getArgument<Runnable>(1).run(); null }.whenever(scheduler).runForPlayer(any(), any())
        val uuid = "00000000-0000-0000-0000-000000000001"
        val db = DatabaseHandler(plugin)
        whenever(plugin.databaseHandler).thenReturn(db)
        try {
            db.openConnection()
            db.createTables()
            val firstStart = System.currentTimeMillis()
            fun punish(start: Long) {
                assertNotNull(db.addPunishment("Target", uuid, "Spam", "Admin", "MUTE", start, start + 600000))
                db.addTemplatePunishmentHistory("Target", uuid, "Spam", "Admin", "MUTE", start, start + 600000, "spam", uuid)
            }
            punish(firstStart)
            val first = db.getPunishments(uuid).single()
            var completed = false
            val service = GuiPunishmentService(plugin)
            service.revoke(operator, first) { completed = true }
            assertTrue(completed)
            assertTrue(db.getPunishments(uuid).isEmpty())
            assertEquals(1, db.countEscalationHistory(uuid, "Target", "Spam", "spam") + 1)
            val history = db.getPunishmentHistory(uuid).single()
            assertTrue(history.undone)
            assertEquals("Reviewer", history.undoneBy)
            assertNotNull(history.undoneAt)
            val template = pl.syntaxdevteam.punisher.templates.PunishTemplate("spam", "Spam", emptyMap())
            assertEquals(1, pl.syntaxdevteam.punisher.gui.player.action.QuickPunishGUI(plugin)
                .nextLevels(java.util.UUID.fromString(uuid), "Target", listOf(template))["spam"])
            val other = pl.syntaxdevteam.punisher.templates.PunishTemplate("other", "Spam", emptyMap())
            db.addTemplatePunishmentHistory("Target", uuid, "Spam", "Admin", "WARN", firstStart, -1, "other", uuid)
            val preview = pl.syntaxdevteam.punisher.gui.player.action.QuickPunishGUI(plugin)
                .nextLevels(java.util.UUID.fromString(uuid), "Target", listOf(template, other))
            assertEquals(1, preview["spam"], "An unrelated template with the same reason must not increase Spam")
            assertEquals(2, preview["other"])
            punish(firstStart + 1000)
            completed = false
            service.revoke(operator, first) { completed = true }
            assertFalse(completed, "A stale GUI entry must not undo the new punishment")
            assertEquals(firstStart + 1000, db.getPunishments(uuid).single().start)
            assertEquals(1, db.countEscalationHistory(uuid, "Target", "Spam", "spam"))
            verify(operator).sendMessage(Component.text("Removed"))
        } finally { db.closeConnection(); folder.deleteRecursively() }
    }
}
