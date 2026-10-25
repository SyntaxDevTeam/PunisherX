package pl.syntaxdevteam.punisher.databases

import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.*

class PunishmentUndoStoreTest {
    private val file = Files.createTempFile("punisher-undo-", ".sqlite")
    private fun connection(): Connection = DriverManager.getConnection("jdbc:sqlite:$file")
    private fun store(scope: String = "network") = PunishmentUndoStore(::connection, scope)
    private val uuid = "00000000-0000-0000-0000-000000000001"

    init {
        connection().use { conn ->
            conn.createStatement().use { statement ->
                for (table in listOf("punishments", "punishmenthistory")) statement.executeUpdate(
                    "CREATE TABLE $table (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, uuid TEXT, reason TEXT, " +
                        "operator TEXT, punishmentType TEXT, start BIGINT, endTime BIGINT, server TEXT)")
                statement.executeUpdate("CREATE TABLE bridge_events (action TEXT, target TEXT, reason TEXT, endTime BIGINT, processed INTEGER)")
                statement.executeUpdate("CREATE TABLE punishment_undos (historyId INTEGER PRIMARY KEY, undoneBy TEXT NOT NULL, undoneAt BIGINT NOT NULL)")
                statement.executeUpdate("CREATE TABLE punishment_templates (historyId INTEGER PRIMARY KEY, templateName TEXT NOT NULL, playerUuid TEXT NOT NULL)")
            }
        }
    }

    @AfterTest fun cleanup() { Files.deleteIfExists(file) }

    private fun sql(sql: String) = connection().use { conn -> conn.createStatement().use { it.executeUpdate(sql) } }
    private fun count(table: String) = connection().use { conn ->
        conn.createStatement().use { statement -> statement.executeQuery("SELECT COUNT(*) FROM $table").use { it.next(); it.getInt(1) } }
    }
    private fun event(start: Long = 100, type: String = "MUTE", template: String = "spam", reason: String = "Spam",
                      server: String = "network", target: String = uuid) {
        store().record("Target", target, reason, "Admin", type, start, -1, server, template, uuid)
        sql("INSERT INTO punishments (name, uuid, reason, operator, punishmentType, start, endTime, server) " +
            "VALUES ('Target', '$target', '$reason', 'Admin', '$type', $start, -1, '$server')")
    }
    private fun undo(template: String = "spam", reason: String = "Spam", scope: String = "network") =
        store(scope).undo(uuid, "Target", reason, template, "Reviewer")
    private fun ladder(template: String = "spam", reason: String = "Spam", scope: String = "network") =
        store(scope).count(uuid, "Target", reason, template)

    @Test fun `undo removes active mute and decrements ladder while retaining original history and audit`() {
        event()
        assertEquals(1, ladder())
        assertEquals("MUTE", undo().single().type)
        assertEquals(0, count("punishments"))
        assertEquals(0, ladder())
        assertEquals(1, count("punishmenthistory"))
        connection().use { conn -> conn.createStatement().use { statement ->
            statement.executeQuery("SELECT * FROM punishment_undos").use {
                assertTrue(it.next()); assertEquals("Reviewer", it.getString("undoneBy")); assertTrue(it.getLong("undoneAt") > 0)
            }
        } }
    }

    @Test fun `latest entry is undone first and repeated calls stop at zero`() {
        event(100); event(200, "BAN")
        assertEquals("BAN", undo().single().type)
        assertEquals(1, ladder())
        assertEquals("MUTE", undo().single().type)
        assertTrue(undo().isEmpty())
        assertEquals(0, ladder())
    }

    @Test fun `already unmuted or expired entry can still be removed from ladder`() {
        event(); sql("DELETE FROM punishments")
        assertEquals(1, ladder())
        assertEquals(1, undo().size)
        assertEquals(0, ladder())
    }

    @Test fun `history ids are independent from active ids and older same type remains active`() {
        sql("INSERT INTO punishments (id) VALUES (50)")
        event(100); event(200)
        undo()
        assertEquals(2, count("punishments"))
        assertEquals(1, ladder())
    }

    @Test fun `templates sharing a reason have independent counters and undo`() {
        event(100, template = "spam"); event(200, template = "chat")
        assertEquals(1, ladder("spam")); assertEquals(1, ladder("chat"))
        assertEquals(100, undo("spam").single().start)
        assertEquals(0, ladder("spam")); assertEquals(1, ladder("chat"))
    }

    @Test fun `renaming reason and player keeps new template records eligible and template names ignore case`() {
        event()
        assertEquals(1, store().count(uuid, "NewName", "Changed reason", "SPAM"))
        assertEquals(1, store().undo(uuid, "NewName", "Changed reason", "SPAM", "Reviewer").size)
    }

    @Test fun `another player or server cannot undo or count the event`() {
        event(server = "survival")
        assertTrue(undo(scope = "creative").isEmpty())
        assertEquals(0, ladder(scope = "creative"))
        assertTrue(store().undo("other-uuid", "Other", "Spam", "spam", "Reviewer").isEmpty())
        assertEquals(1, ladder(scope = "survival"))
        assertEquals(1, undo(scope = "survival").size)
    }

    @Test fun `server specific commands can undo network events`() {
        event()
        assertEquals(1, undo(scope = "survival").size)
    }

    @Test fun `multi IP ban counts once and reverses all IPs from that event`() {
        event(100, "BANIP", target = "192.0.2.1")
        event(100, "BANIP", target = "192.0.2.2")
        event(200, "BANIP", template = "other", target = "192.0.2.1")
        assertEquals(1, ladder())
        assertEquals(2, undo().size)
        assertEquals(0, ladder())
        assertEquals(1, count("punishments"))
        assertEquals(1, ladder("other"))
    }

    @Test fun `undo cancels a queued disconnect for a removed ban`() {
        event(type = "BAN")
        sql("INSERT INTO bridge_events VALUES ('BAN', '$uuid', 'Spam', -1, 0)")
        undo()
        assertEquals(0, count("bridge_events"))
    }

    @Test fun `queued disconnect remains when another ban is still active`() {
        event(100, "BAN", template = "other"); event(200, "BAN")
        sql("INSERT INTO bridge_events VALUES ('BAN', '$uuid', 'Spam', -1, 0)")
        undo()
        assertEquals(1, count("bridge_events"))
    }

    @Test fun `kick history contributes a reversible ladder step`() {
        event(type = "KICK"); sql("DELETE FROM punishments")
        assertEquals(1, ladder())
        assertEquals("KICK", undo().single().type)
        assertEquals(0, ladder())
    }

    @Test fun `legacy history is matched by reason and newest entry wins`() {
        event(100); sql("DELETE FROM punishment_templates")
        event(200)
        assertEquals(2, ladder())
        assertEquals(200, undo().single().start)
        assertEquals(100, undo().single().start)
    }

    @Test fun `failure removing active punishment rolls back audit and ladder change`() {
        event()
        sql("CREATE TRIGGER fail_delete BEFORE DELETE ON punishments BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        assertFails { undo() }
        assertEquals(1, count("punishments"))
        assertEquals(0, count("punishment_undos"))
        assertEquals(1, ladder())
    }

    @Test fun `template identity failure also rolls back history insertion`() {
        sql("CREATE TRIGGER fail_template BEFORE INSERT ON punishment_templates BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        assertFails { event() }
        assertEquals(0, count("punishmenthistory"))
        assertEquals(0, count("punishment_templates"))
    }
}
