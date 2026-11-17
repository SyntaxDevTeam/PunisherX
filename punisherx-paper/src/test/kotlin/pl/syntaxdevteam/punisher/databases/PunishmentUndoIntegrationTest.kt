package pl.syntaxdevteam.punisher.databases

import org.bukkit.configuration.file.YamlConfiguration
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.*

class PunishmentUndoIntegrationTest {
    @Test fun `plugin schemas support persistent undo history display and export`() {
        val folder = Files.createTempDirectory("punisher-undo-integration-").toFile()
        val plugin = mock<PunisherX>()
        val config = YamlConfiguration().apply {
            set("database.type", "sqlite")
            set("database.sql.dbname", "test.sqlite")
            set("server", "survival")
        }
        whenever(plugin.config).thenReturn(config)
        whenever(plugin.dataFolder).thenReturn(folder)
        whenever(plugin.logger).thenReturn(mock())
        whenever(plugin.discordWebhook).thenReturn(mock())
        val uuid = "00000000-0000-0000-0000-000000000001"
        val database = DatabaseHandler(plugin)
        try {
            database.openConnection()
            database.createTables()
            assertNotNull(database.addPunishment("Target", uuid, "Spam", "Admin", "MUTE", 100, -1))
            database.addTemplatePunishmentHistory("Target", uuid, "Spam", "Admin", "MUTE", 100, -1, "spam", uuid)
            assertEquals(1, database.countEscalationHistory(uuid, "Target", "Spam", "spam"))
            assertFalse(database.getPunishmentHistory(uuid).single().undone)
            assertEquals(1, database.undoPunishment(uuid, "Target", "Spam", "spam", "Reviewer")?.size)
            assertEquals(0, database.countEscalationHistory(uuid, "Target", "Spam", "spam"))
            assertTrue(database.getPunishmentHistory(uuid).single().undone)
            database.exportDatabase()
            val dump = folder.resolve("dump/backup.sql").readText()
            assertContains(dump, "INSERT INTO punishment_undos")
            assertContains(dump, "INSERT INTO punishment_templates")
            DriverManager.getConnection("jdbc:sqlite:${folder.resolve("test.sqlite")}").use { connection ->
                for (table in listOf("punishment_undos", "punishment_templates")) {
                    connection.createStatement().use { statement ->
                        statement.executeQuery("PRAGMA table_info($table)").use { rows ->
                            var columns = 0
                            while (rows.next()) columns++
                            assertEquals(3, columns, "The audit and identity schemas must match database migration schemas")
                        }
                    }
                }
            }
            database.deletePlayerData(uuid)
            assertTrue(database.getPunishmentHistory(uuid).isEmpty())
        } finally {
            database.closeConnection()
            folder.deleteRecursively()
        }
    }
}
