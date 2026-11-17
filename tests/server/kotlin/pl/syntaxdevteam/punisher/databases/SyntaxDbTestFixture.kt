package pl.syntaxdevteam.punisher.databases

import org.bukkit.configuration.file.YamlConfiguration
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.punisher.basic.PunishmentManager
import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.AfterTest

/** Each test owns a real file and SyntaxDB pool; JDBC assertions do not reuse production queries. */
abstract class SyntaxDbTestFixture {
    protected val folder = Files.createTempDirectory("punisher-contract-").toFile()
    protected val config = YamlConfiguration().apply {
        set("database.type", "sqlite")
        set("database.sql.dbname", "contract.sqlite")
        set("server", "survival")
    }
    protected val plugin = mock<PunisherX>().also {
        whenever(it.config).thenReturn(config)
        whenever(it.dataFolder).thenReturn(folder)
        whenever(it.name).thenReturn("PunisherX")
        whenever(it.logger).thenReturn(mock())
        whenever(it.discordWebhook).thenReturn(mock())
        whenever(it.punishmentManager).thenReturn(PunishmentManager())
        whenever(it.server).thenReturn(mock())
        whenever(it.server.logger).thenReturn(java.util.logging.Logger.getAnonymousLogger())
    }
    protected val handler = DatabaseHandler(plugin)
    protected val uuid = UUID.randomUUID()
    protected val reason = "Zażółć 🐉 O'Brien;\nC:\\players\\data"

    protected fun start() {
        handler.openConnection()
        handler.createTables()
        whenever(plugin.databaseHandler).thenReturn(handler)
    }

    protected fun <T> jdbc(block: (Connection) -> T): T =
        DriverManager.getConnection("jdbc:sqlite:${folder.resolve("contract.sqlite")}").use(block)

    protected fun sql(statement: String) = jdbc { connection ->
        connection.createStatement().use { it.execute(statement) }
    }

    protected fun count(table: String): Int = jdbc { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM $table").use { it.next(); it.getInt(1) }
        }
    }

    protected fun add(type: String = "MUTE", scope: String = "survival", start: Long = 100,
                      end: Long = -1, target: String = uuid.toString(), name: String = "Target"): Int =
        requireNotNull(handler.addPunishment(name, target, reason, "Admin", type, start, end, scope)).toInt()

    @AfterTest fun closeDatabase() {
        try { handler.closeConnection() } finally { folder.deleteRecursively() }
    }
}
