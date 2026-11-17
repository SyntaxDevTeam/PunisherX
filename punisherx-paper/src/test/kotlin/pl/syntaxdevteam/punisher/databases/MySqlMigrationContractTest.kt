package pl.syntaxdevteam.punisher.databases

import org.junit.jupiter.api.Assumptions.assumeTrue
import pl.syntaxdevteam.syntaxdb.Database
import java.nio.file.Files
import kotlin.test.*

/** Opt-in live contract; use an EMPTY disposable schema, never a production database. */
class MySqlMigrationContractTest {
    @Test fun `MySQL driver on MariaDB supports Unicode keys undo and bidirectional SQLite transfer`() {
        val url = System.getenv("PUNISHERX_TEST_MYSQL_URL")
        assumeTrue(!url.isNullOrBlank(), "Set PUNISHERX_TEST_MYSQL_URL to an empty disposable database")
        val folder = Files.createTempDirectory("punisher-live-mysql-").toFile()
        Database.connect("mysql") {
            jdbcUrl = url; username = "root"; password = ""
            property("characterEncoding", "UTF-8")
        }.use { mysql ->
            PunisherDatabaseSchema.create(mysql)
            assertTrue(PunisherDatabaseSchema.tables.all { mysql.count(it) == 0L }, "Use an empty disposable schema")
            val reason = "Zażółć 🐉 O'Brien;\nC:\\cache"
            val id = mysql.insert("punishments", mapOf("name" to "Target", "uuid" to "uuid", "reason" to reason,
                "operator" to "Admin", "punishmentType" to "BAN", "start" to 100L, "endTime" to -1L, "server" to "network")).generatedKey
            assertNotNull(id)
            val store = PunishmentUndoStore(mysql, "network")
            store.record("Target", "uuid", reason, "Admin", "BAN", 100, -1, "network", "spam", "uuid")
            assertEquals(1, store.count("uuid", "Target", reason, "spam"))
            mysql.execute("INSERT INTO bridge_events (action, target, reason, endTime) VALUES ('BAN', 'uuid', ?, -1)", listOf(reason))
            mysql.execute("INSERT INTO reports (player, suspect, reason) VALUES ('uuid', 'uuid2', ?)", listOf(reason))
            val snapshot = DatabaseBackup.snapshot(mysql)
            val dump = folder.resolve("backup.sql")
            DatabaseBackup.write(snapshot, dump)
            Database.connect("sqlite") { file = folder.resolve("copy.sqlite").path }.use { sqlite ->
                PunisherDatabaseSchema.create(sqlite)
                DatabaseBackup.restore(sqlite, DatabaseBackup.read(dump))
                assertEquals(reason, sqlite.query("SELECT reason FROM punishments").single().string("reason"))
                assertEquals(1L, sqlite.count("reports"))
                mysql.transaction { PunisherDatabaseSchema.tables.reversed().forEach { deleteAll(it) } }
                DatabaseBackup.restore(mysql, DatabaseBackup.snapshot(sqlite))
            }
            assertEquals(reason, mysql.query("SELECT reason FROM punishments").single().string("reason"))
            assertEquals(1, store.undo("uuid", "Target", reason, "spam", "Reviewer").size)
            assertEquals(0L, mysql.count("punishments")); assertEquals(0L, mysql.count("bridge_events"))
            assertEquals(1L, mysql.count("punishment_undos"))
            // Leave the disposable schema empty so this test can be rerun.
            mysql.transaction { PunisherDatabaseSchema.tables.reversed().forEach { deleteAll(it) } }
        }
        folder.deleteRecursively()
    }
}
