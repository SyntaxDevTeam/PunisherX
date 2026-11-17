package pl.syntaxdevteam.punisher.databases

import org.bukkit.configuration.file.YamlConfiguration
import org.mockito.kotlin.*
import pl.syntaxdevteam.punisher.PunisherX
import pl.syntaxdevteam.syntaxdb.Database
import pl.syntaxdevteam.syntaxdb.config.DatabaseType
import java.nio.file.Files
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.*

class SyntaxDbMigrationTest {
    private val folder = Files.createTempDirectory("punisher-syntaxdb-").toFile()
    private val config = YamlConfiguration().apply {
        set("database.type", "sqlite"); set("database.sql.dbname", "existing.sqlite")
        set("server", "survival")
    }
    private val plugin = mock<PunisherX>().also {
        whenever(it.config).thenReturn(config); whenever(it.dataFolder).thenReturn(folder)
        whenever(it.name).thenReturn("PunisherX"); whenever(it.logger).thenReturn(mock())
        whenever(it.discordWebhook).thenReturn(mock())
    }
    private val handler = DatabaseHandler(plugin)
    @AfterTest fun cleanup() { handler.closeConnection(); folder.deleteRecursively() }

    @Test fun `existing sqlite path and legacy rows survive upgrade and missing scope column is backfilled`() {
        DriverManager.getConnection("jdbc:sqlite:${folder.resolve("existing.sqlite")}").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE punishmenthistory (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, uuid TEXT, reason TEXT, operator TEXT, punishmentType TEXT, start BIGINT, endTime BIGINT)")
                statement.execute("INSERT INTO punishmenthistory VALUES (42, 'Target', 'uuid', 'Legacy', 'Admin', 'MUTE', 100, -1)")
            }
        }
        handler.openConnection(); handler.createTables()
        assertTrue(handler.isReady()); assertTrue(handler.runHealthCheck().ok)
        val history = handler.getPunishmentHistory("uuid").single()
        assertEquals(42, history.id); assertEquals("Legacy", history.reason); assertEquals("network", history.server)
        handler.addPunishmentHistory("Target", "uuid", "New", "Admin", "MUTE", 200, -1)
        assertEquals(listOf(43, 42), handler.getPunishmentHistory("uuid", 10, 0).map { it.id })
        assertFalse(folder.resolve("existing.sqlite.db").exists())
    }

    @Test fun `single punishment removal does not delete tied timestamps or another server`() {
        handler.openConnection(); handler.createTables()
        val one = handler.addPunishment("Target", "uuid", "A", "Admin", "MUTE", 100, -1)
        val two = handler.addPunishment("Target", "uuid", "B", "Admin", "MUTE", 100, -1)
        handler.addPunishment("Target", "uuid", "C", "Admin", "MUTE", 100, -1, "creative")
        handler.removePunishment("uuid", "MUTE")
        DriverManager.getConnection("jdbc:sqlite:${folder.resolve("existing.sqlite")}").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT id FROM punishments ORDER BY id").use { rows ->
                    val ids = buildList { while (rows.next()) add(rows.getLong(1)) }
                    assertNotNull(one); assertNotNull(two); assertEquals(2, ids.size)
                    assertTrue(one in ids); assertFalse(two in ids)
                }
            }
        }
    }

    @Test fun `reports retain timestamps and nullable resolution fields through SyntaxDB rows`() {
        handler.openConnection(); handler.createTables()
        val reporter = UUID.randomUUID(); val suspect = UUID.randomUUID()
        assertTrue(handler.addReport(reporter, suspect, "Zażółć 🐉 'quoted';\nsecond line"))
        val report = handler.getReports(10, 0).single()
        assertEquals(reporter, report.player); assertEquals("OPEN", report.status)
        assertNull(report.handledAt); assertNull(report.note); assertTrue(report.filedAt.epochSecond > 0)
        assertTrue(handler.resolveReport(report.id, "RESOLVED", "Admin", "Done"))
        assertTrue(handler.getReports().isEmpty())
        assertEquals("RESOLVED", handler.getReports(closed = true).single().status)
    }

    @Test fun `shutdown rejects operations and closes idempotently`() {
        handler.openConnection(); handler.createTables(); handler.closeConnection(); handler.closeConnection()
        assertFalse(handler.isReady()); assertFalse(handler.runHealthCheck().ok)
        assertFails { handler.replacePlayerCacheLinesTransactional(listOf("after shutdown")) }
    }

    @Test fun `SQL Server pagination preserves parameter order`() {
        config.set("database.type", "mssql"); config.set("database.sql.dbname", "punisherx")
        val sqlServer = DatabaseHandler(plugin)
        try {
            val method = DatabaseHandler::class.java.getDeclaredMethod("paginatedSql", String::class.java, List::class.java)
            method.isAccessible = true
            val result = method.invoke(sqlServer, "SELECT * FROM reports WHERE player = ? ORDER BY id LIMIT ? OFFSET ?", listOf("uuid", 20, 40)) as Pair<*, *>
            assertEquals("SELECT * FROM reports WHERE player = ? ORDER BY id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", result.first)
            assertEquals(listOf("uuid", 40, 20), result.second)
            val fixed = method.invoke(sqlServer, "SELECT * FROM punishments ORDER BY start DESC LIMIT 1", emptyList<Any>()) as Pair<*, *>
            assertEquals(listOf(0, 1), fixed.second)
        } finally { sqlServer.closeConnection() }
    }

    @Test fun `configuration keeps MariaDB alias engine ports and rejects H2 explicitly`() {
        assertEquals(DatabaseType.MYSQL, PunisherDatabaseSettings.parseType("MariaDB"))
        assertEquals(DatabaseType.SQL_SERVER, PunisherDatabaseSettings.parseType("MSSQL"))
        assertEquals(DatabaseType.POSTGRESQL, PunisherDatabaseSettings.parseType("pgsql"))
        assertContains(assertFailsWith<IllegalArgumentException> { PunisherDatabaseSettings.parseType("h2") }.message.orEmpty(), "before upgrading")
        assertEquals("jdbc:sqlite:${folder.resolve("existing.sqlite")}", PunisherDatabaseSettings.create(config, folder, "PunisherX", DatabaseType.SQLITE).jdbcUrl)
        config.set("database.sql.port", 0)
        assertContains(PunisherDatabaseSettings.create(config, folder, "PunisherX", DatabaseType.POSTGRESQL).jdbcUrl, ":5432/")
    }

    @Test fun `complete backup round trip preserves Unicode newlines nulls and all ten tables`() {
        Database.connect("sqlite") { file = folder.resolve("source.sqlite").path }.use { source ->
            PunisherDatabaseSchema.create(source)
            val text = "Zażółć 🐉 O'Brien;\nC:\\players\\data"
            source.execute("INSERT INTO punishments (id, reason, server) VALUES (?, ?, ?)", listOf(42, text, "network"))
            source.execute("INSERT INTO punishmenthistory (id, reason) VALUES (?, ?)", listOf(17, text))
            source.execute("INSERT INTO punishment_undos VALUES (17, 'Admin', 123)")
            source.execute("INSERT INTO punishment_templates VALUES (17, 'spam', 'uuid')")
            source.execute("INSERT INTO playercache (data) VALUES (?)", listOf(text))
            source.execute("INSERT INTO reports (player, suspect, reason) VALUES ('uuid', 'uuid2', ?)", listOf(text))
            source.execute("INSERT INTO report_resolutions VALUES (1, 'RESOLVED', 'Admin', 'Done', 123)")
            source.execute("INSERT INTO bridge_events (action, target, reason, endTime) VALUES ('BAN', 'uuid', ?, -1)", listOf(text))
            source.execute("INSERT INTO punisherx_identity_migrations VALUES ('id', 'source', 'target', 'COMPLETED')")
            source.execute("INSERT INTO punisherx_identity_migration_items VALUES ('id', 'HISTORY', '17')")
            val backup = folder.resolve("backup.sql")
            val snapshot = DatabaseBackup.snapshot(source)
            DatabaseBackup.write(snapshot, backup)
            Database.connect("sqlite") { file = folder.resolve("target.sqlite").path }.use { target ->
                PunisherDatabaseSchema.create(target)
                DatabaseBackup.restore(target, DatabaseBackup.read(backup))
                assertEquals(snapshot, DatabaseBackup.snapshot(target))
                assertEquals(43L, (target.insert("punishments", mapOf("reason" to "next")).generatedKey as Number).toLong())
            }
        }
    }

    @Test fun `restore rolls back earlier tables on duplicate primary key and rejects arbitrary SQL`() {
        Database.connect("sqlite") { file = folder.resolve("rollback.sqlite").path }.use { target ->
            PunisherDatabaseSchema.create(target)
            target.execute("INSERT INTO punishment_undos VALUES (7, 'Existing', 1)")
            assertFails { DatabaseBackup.restore(target, linkedMapOf(
                "playercache" to listOf(mapOf("id" to 1, "data" to "must roll back")),
                "punishment_undos" to listOf(mapOf("historyId" to 7, "undoneBy" to "Other", "undoneAt" to 2))
            )) }
            assertEquals(0L, target.count("playercache")); assertEquals(1L, target.count("punishment_undos"))
            val malicious = folder.resolve("bad.sql").apply { writeText("DELETE FROM punishments;") }
            assertFails { DatabaseBackup.read(malicious) }
            malicious.writeText("INSERT INTO playercache (id, data) VALUES (1, 'unterminated);")
            assertFails { DatabaseBackup.read(malicious) }
        }
    }

    @Test fun `legacy columnless dump accepts quoted numeric values and multiline strings`() {
        val dump = folder.resolve("legacy.sql").apply { writeText("INSERT INTO playercache VALUES\n('8', 'old;\nO''Brien'),\n('9', NULL);\n") }
        Database.connect("sqlite") { file = folder.resolve("legacy.sqlite").path }.use { target ->
            PunisherDatabaseSchema.create(target)
            DatabaseBackup.restore(target, DatabaseBackup.read(dump))
            val rows = target.query("SELECT * FROM playercache ORDER BY id")
            assertEquals(8, rows[0].int("id")); assertEquals("old;\nO'Brien", rows[0].string("data"))
            assertNull(rows[1]["data"])
        }
    }
}
