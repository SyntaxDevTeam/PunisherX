package pl.syntaxdevteam.punisher.commands

import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.compatibility.platform.SchedulerAdapter
import pl.syntaxdevteam.punisher.databases.SyntaxDbTestFixture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

/** Actual root commands and SQLite; scheduled work and replies are drained independently. */
abstract class DatabaseBackupCommandContract : SyntaxDbTestFixture() {
    private val scheduler = mock<SchedulerAdapter>()
    private val sender = mock<ConsoleCommandSender>()
    private val workers = ArrayDeque<Runnable>()
    private val replies = ArrayDeque<Runnable>()
    private val backup get() = folder.resolve("dump/backup.sql")

    protected abstract fun executeRoot(sender: CommandSender, operation: String)

    @BeforeTest fun prepareBackupCommands() {
        start()
        whenever(plugin.isEnabled).thenReturn(true)
        whenever(plugin.schedulerAdapter).thenReturn(scheduler)
        whenever(plugin.messageHandler).thenReturn(mock())
        whenever(plugin.messageHandler.stringMessageToComponent(any(), any(), any<Map<String, String>>()))
            .thenReturn(Component.empty())
        whenever(sender.spigot()).thenReturn(mock())
        doAnswer { workers.addLast(it.getArgument(0)); null }.whenever(scheduler).runAsync(any())
        doAnswer { replies.addLast(it.getArgument(0)); null }.whenever(scheduler).runSync(any())
        doAnswer { replies.addLast(it.getArgument(1)); null }.whenever(scheduler).runForPlayer(any(), any())
    }

    private fun runWorker() {
        val worker = workers.removeFirst()
        val executor = Executors.newSingleThreadExecutor()
        try { executor.submit(worker).get(30, TimeUnit.SECONDS) }
        finally { executor.shutdownNow() }
    }

    private fun message(key: String, times: Int = 1) {
        verify(plugin.messageHandler, times(times)).stringMessageToComponent(
            eq("database-backup"), eq(key), any<Map<String, String>>())
    }

    @Test fun `export is deferred and success is delivered only through reply scheduler`() {
        add()
        executeRoot(sender, "export")
        message("export-started")
        assertFalse(backup.exists())
        runWorker()
        assertTrue(backup.readText().contains(reason.replace("'", "''")))
        message("export-success", 0)
        replies.removeFirst().run()
        message("export-success")
        verify(plugin.server, never()).scheduler
    }

    @Test fun `second backup operation is rejected across different senders until worker finishes`() {
        executeRoot(sender, "export")
        val other = mock<ConsoleCommandSender>()
        whenever(other.spigot()).thenReturn(mock())
        executeRoot(other, "import")
        assertEquals(1, workers.size)
        message("busy")
        runWorker()
        replies.removeFirst().run()
        executeRoot(other, "import")
        assertEquals(1, workers.size)
        runWorker()
        replies.removeFirst().run()
        message("import-success")
    }

    @Test fun `export IO error is handled and command can be retried`() {
        folder.resolve("dump").writeText("not a directory")
        executeRoot(sender, "export")
        runWorker()
        replies.removeFirst().run()
        message("export-failed")
        verify(plugin).reportError(any())
        folder.resolve("dump").delete()
        executeRoot(sender, "export")
        runWorker()
        replies.removeFirst().run()
        message("export-success")
    }

    @Test fun `successful import appends records without clearing existing data`() {
        add()
        backup.parentFile.mkdirs()
        backup.writeText("INSERT INTO playercache (id, data) VALUES (42, 'O''Brien');")
        executeRoot(sender, "import")
        assertEquals(0, count("playercache"))
        runWorker()
        assertEquals(1, count("playercache"))
        assertEquals(1, count("punishments"))
        message("import-success", 0)
        replies.removeFirst().run()
        message("import-success")
    }

    @Test fun `key conflict rolls back earlier inserts and reports failure without success`() {
        val id = add()
        backup.parentFile.mkdirs()
        backup.writeText("""
            INSERT INTO playercache (id, data) VALUES (999, 'must roll back');
            INSERT INTO punishments (id, name, uuid, reason, operator, punishmentType, start, endTime, server)
              VALUES ($id, 'Target', '$uuid', 'duplicate', 'Admin', 'MUTE', 100, -1, 'survival');
        """.trimIndent())
        executeRoot(sender, "import")
        runWorker()
        replies.removeFirst().run()
        assertEquals(0, count("playercache"))
        assertEquals(reason, handler.getPunishments(uuid.toString()).single().reason)
        message("import-failed")
        message("import-success", 0)
    }

    @Test fun `missing and malformed backups produce handled import errors and permit retry`() {
        executeRoot(sender, "import")
        runWorker()
        replies.removeFirst().run()
        backup.parentFile.mkdirs()
        backup.writeText("DROP TABLE punishments;")
        executeRoot(sender, "import")
        runWorker()
        replies.removeFirst().run()
        message("import-failed", 2)
        assertEquals(0, count("punishments"))
        backup.writeText("INSERT INTO playercache VALUES (42, 'valid');")
        executeRoot(sender, "import")
        runWorker()
        replies.removeFirst().run()
        message("import-success")
    }

    @Test fun `scheduler rejection is reported immediately and releases busy state`() {
        doThrow(IllegalStateException("scheduler stopped")).whenever(scheduler).runAsync(any())
        repeat(2) { executeRoot(sender, "export") }
        message("export-failed", 2)
        message("busy", 0)
        assertFalse(backup.exists())
    }

    @Test fun `player result uses player scheduler`() {
        val player = mock<Player>()
        whenever(player.hasPermission(any<String>())).thenReturn(true)
        whenever(player.spigot()).thenReturn(mock())
        executeRoot(player, "export")
        runWorker()
        verify(scheduler).runForPlayer(eq(player), any())
        verify(scheduler, never()).runSync(any())
        replies.removeFirst().run()
        message("export-success")
    }

    @Test fun `disabled plugin does not schedule a late response`() {
        executeRoot(sender, "export")
        whenever(plugin.isEnabled).thenReturn(false)
        runWorker()
        assertTrue(replies.isEmpty())
    }

    @Test fun `sender without permissions cannot schedule import or export`() {
        val denied = mock<CommandSender>()
        whenever(denied.spigot()).thenReturn(mock())
        for (operation in listOf("import", "export")) executeRoot(denied, operation)
        assertTrue(workers.isEmpty())
        assertFalse(backup.exists())
        verifyNoInteractions(scheduler)
    }
}
