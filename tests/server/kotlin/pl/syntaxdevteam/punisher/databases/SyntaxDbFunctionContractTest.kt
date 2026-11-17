package pl.syntaxdevteam.punisher.databases

import org.mockito.kotlin.*
import pl.syntaxdevteam.syntaxdb.config.DatabaseType
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

class SyntaxDbFunctionContractTest : SyntaxDbTestFixture() {
    @Test fun `all punishment types preserve generated IDs and every mapped field`() {
        start()
        val types = listOf("BAN", "BANIP", "MUTE", "WARN", "JAIL")
        val ids = types.mapIndexed { index, type -> add(type, start = 100L + index) }
        assertEquals(ids.size, ids.toSet().size)
        val rows = handler.getPunishments(uuid.toString())
        assertEquals(types.toSet(), rows.map { it.type }.toSet())
        rows.forEach { row ->
            assertEquals(uuid.toString(), row.uuid)
            assertEquals(reason, row.reason)
            assertEquals("Admin", row.operator)
            assertEquals("Target", row.name)
            assertEquals("survival", row.server)
            assertEquals(-1L, row.end)
            assertTrue(row.id in ids)
        }
        verify(plugin.discordWebhook, times(types.size)).sendPunishmentWebhook(any(), eq("Target"), eq("Admin"), eq(reason), any(), any(), eq(-1L))
    }

    @Test fun `expired row cleanup preserves newer active punishment of the same type`() {
        start()
        val expired = add(start = 10, end = 20)
        val active = add(start = 30)
        assertEquals(listOf(active), handler.getPunishments(uuid.toString()).map { it.id })
        assertEquals(listOf(active), handler.getPunishments(uuid.toString()).map { it.id })
        jdbc { c -> c.createStatement().use { s -> s.executeQuery("SELECT id FROM punishments").use {
            assertTrue(it.next()); assertEquals(active, it.getInt(1)); assertFalse(it.next())
        } } }
        assertNotEquals(expired, active)
    }

    @Test fun `multiple expired records do not consume active network or local rows`() {
        start()
        repeat(3) { add("WARN", start = it.toLong(), end = 1) }
        val local = add("WARN", start = 20)
        val network = add("WARN", "network", start = 30)
        add("WARN", "creative", end = 1)
        repeat(2) { assertEquals(setOf(local, network), handler.getPunishments(uuid.toString()).map { it.id }.toSet()) }
        assertEquals(3, count("punishments"))
    }

    @Test fun `local scope includes network and excludes other servers on reads and removal`() {
        start()
        val local = add("BAN")
        val network = add("BAN", "network")
        val foreign = add("BAN", "creative")
        assertEquals(setOf(local, network), handler.getBannedPlayers(10, 0).map { it.id }.toSet())
        assertEquals(2, handler.countAllPunishments())
        handler.removePunishment(uuid.toString(), "BAN", true)
        assertEquals(1, count("punishments"))
        config.set("server", "network")
        assertEquals(listOf(foreign), handler.getPunishments(uuid.toString()).map { it.id })
    }

    @Test fun `scope trims whitespace and empty scope defaults to network`() {
        start()
        config.set("server", " SuRvIvAl ")
        assertEquals("survival", handler.serverScope())
        add(scope = "creative")
        assertTrue(handler.getPunishments(uuid.toString()).isEmpty())
        config.set("server", "  ")
        assertEquals("network", handler.serverScope())
        assertEquals(1, handler.getPunishments(uuid.toString()).size)
    }

    @Test fun `remove by ID reports missing or inaccessible rows accurately`() {
        start()
        val local = add()
        val foreign = add(scope = "creative")
        assertFalse(handler.removePunishmentById(foreign))
        assertFalse(handler.removePunishmentById(Int.MAX_VALUE))
        assertTrue(handler.removePunishmentById(local))
        assertFalse(handler.removePunishmentById(local))
        assertEquals(1, count("punishments"))
    }

    @Test fun `active warning counts exclude expiration foreign scope and other types`() {
        start()
        add("WARN"); add("WARN", "network")
        add("WARN", end = 1); add("WARN", "creative"); add("BAN")
        assertEquals(2, handler.getActiveWarnCount(uuid.toString()))
        assertEquals(0, handler.getActiveWarnCount(UUID.randomUUID().toString()))
    }

    @Test fun `IP lookup retains Unicode and filters inactive and foreign bans`() {
        start()
        val ip = "2001:db8::42"
        val active = add("BANIP", target = ip)
        add("BANIP", target = ip, end = 1)
        add("BANIP", "creative", target = ip)
        val row = handler.getPunishmentsByIP(ip).single()
        assertEquals(active, row.id); assertEquals(ip, row.uuid); assertEquals(reason, row.reason)
        assertTrue(handler.getPunishmentsByIP("' OR 1=1 --").isEmpty())
    }

    @Test fun `pagination is stable for tied punishment timestamps`() {
        start()
        val ids = (1..6).map { add(start = 100) }.reversed()
        assertEquals(ids.take(2), handler.getPunishments(uuid.toString(), 2, 0).map { it.id })
        assertEquals(ids.drop(2).take(2), handler.getPunishments(uuid.toString(), 2, 2).map { it.id })
        assertTrue(handler.getPunishments(uuid.toString(), 2, 99).isEmpty())
    }

    @Test fun `history pagination last ten and statistics preserve scope and ordering`() {
        start()
        repeat(12) { handler.addPunishmentHistory("Target", uuid.toString(), "Reason $it", "Admin", "BAN", it.toLong(), -1) }
        handler.addPunishmentHistory("Target", uuid.toString(), "Other server", "Admin", "BAN", 1000, -1, "creative")
        assertEquals(listOf("Reason 9", "Reason 8", "Reason 7"), handler.getPunishmentHistory(uuid.toString(), 3, 2).map { it.reason })
        assertEquals((11 downTo 2).map { "Reason $it" }, handler.getLastTenPunishments(uuid.toString()).map { it.reason })
        assertEquals(12, handler.countAllPunishmentHistory())
        assertEquals(12, handler.countPlayerAllPunishmentHistory(uuid))
        assertEquals(3, handler.getHistoryBannedPlayers(3, 0).size)
        assertEquals(0, handler.countPlayerAllPunishmentHistory(UUID.randomUUID()))
    }

    @Test fun `history with tied timestamps has deterministic pages`() {
        start()
        repeat(5) { handler.addPunishmentHistory("Target", uuid.toString(), "Reason $it", "Admin", "WARN", 100, -1) }
        assertEquals(listOf("Reason 4", "Reason 3"), handler.getPunishmentHistory(uuid.toString(), 2, 0).map { it.reason })
        assertEquals(listOf("Reason 2", "Reason 1"), handler.getPunishmentHistory(uuid.toString(), 2, 2).map { it.reason })
    }

    @Test fun `GUI filters combine case insensitive names types scope and offset`() {
        start()
        val one = add("BAN", start = 10, name = "Alice")
        val two = add("BANIP", "network", 20, name = "ALICE")
        add("MUTE", start = 30, name = "Alice")
        add("BAN", "creative", 40, name = "Alice")
        add("BAN", start = 50, name = "Bob")
        add("BAN", start = 60, end = 1, name = "Alice")
        assertEquals(setOf(one, two), handler.getActivePunishmentsFiltered(setOf("ban", "banip"), "aLiCe", 20, 0).map { it.id }.toSet())
        assertEquals(listOf(one), handler.getActivePunishmentsFiltered(setOf("BANIP", "BAN"), "alice", 20, 2).map { it.id })
        assertTrue(handler.getActivePunishmentsFiltered(emptySet(), "' OR 1=1 --", 20, 0).isEmpty())
        assertEquals(4, handler.getActivePunishmentsFiltered(emptySet(), "", 20, 0).size)
    }

    @Test fun `jail and ban lists exclude unrelated types and honor pagination`() {
        start()
        val jail = add("JAIL", start = 1)
        val ban = add("BAN", start = 2)
        val ip = add("BANIP", "network", start = 3)
        add("MUTE", start = 4); add("JAIL", "creative", start = 5)
        assertEquals(listOf(jail), handler.getJailedPlayers(10, 0).map { it.id })
        assertEquals(listOf(ip, ban), handler.getBannedPlayers(10, 0).map { it.id })
        assertEquals(listOf(ban), handler.getBannedPlayers(1, 1).map { it.id })
    }

    @Test fun `today statistics use midnight and keep legacy active plus history count`() {
        start()
        val midnight = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        add(start = midnight); add(start = midnight - 1); add(scope = "creative", start = midnight)
        handler.addPunishmentHistory("Target", uuid.toString(), reason, "Admin", "MUTE", midnight, -1)
        assertEquals(2, handler.countTodayPunishments())
    }

    @Test fun `changing reason is scoped and distinguishes nonexistent history`() {
        start()
        handler.addPunishmentHistory("Target", uuid.toString(), "Old", "Admin", "MUTE", 1, -1)
        handler.addPunishmentHistory("Target", uuid.toString(), "Foreign", "Admin", "MUTE", 1, -1, "creative")
        assertTrue(handler.updatePunishmentReason(1, reason))
        assertEquals(reason, handler.getPunishmentHistory(uuid.toString()).single().reason)
        assertFalse(handler.updatePunishmentReason(2, "Must not change"))
        assertFalse(handler.updatePunishmentReason(999, "Missing"))
        config.set("server", "network")
        assertEquals("Foreign", handler.getPunishmentHistory(uuid.toString()).first { it.id == 2 }.reason)
    }

    @Test fun `deleting player removes template and undo references only in accessible scopes`() {
        start()
        for (scope in listOf("survival", "network", "creative")) {
            add(scope = scope)
            handler.addTemplatePunishmentHistory("Target", uuid.toString(), reason, "Admin", "MUTE", 1, -1, "spam", uuid.toString(), scope)
        }
        sql("INSERT INTO punishment_undos VALUES (1, 'Admin', 100)")
        handler.deletePlayerData(uuid.toString())
        assertEquals(1, count("punishments")); assertEquals(1, count("punishmenthistory"))
        assertEquals(1, count("punishment_templates")); assertEquals(0, count("punishment_undos"))
    }

    @Test fun `failed delete rolls back active records history and references together`() {
        start()
        add()
        handler.addTemplatePunishmentHistory("Target", uuid.toString(), reason, "Admin", "MUTE", 1, -1, "spam", uuid.toString())
        sql("CREATE TRIGGER reject_history_delete BEFORE DELETE ON punishmenthistory BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        handler.deletePlayerData(uuid.toString())
        assertEquals(1, count("punishments")); assertEquals(1, count("punishmenthistory")); assertEquals(1, count("punishment_templates"))
    }

    @Test fun `cache replacement preserves order Unicode empty strings and clears on empty input`() {
        start()
        handler.savePlayerCacheLine("old")
        handler.overwritePlayerCache(listOf(reason, "", "last"))
        assertEquals(listOf(reason, "", "last"), handler.getPlayerCacheLines())
        handler.overwritePlayerCache(emptyList())
        assertEquals(emptyList(), handler.getPlayerCacheLines())
    }

    @Test fun `cache transaction restores old lines when an insert fails after delete`() {
        start()
        handler.savePlayerCacheLine("original")
        sql("CREATE TRIGGER reject_cache BEFORE INSERT ON playercache WHEN NEW.data = 'reject' BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        assertFails { handler.replacePlayerCacheLinesTransactional(listOf("first", "reject", "last")) }
        assertEquals(listOf("original"), handler.getPlayerCacheLines())
    }

    @Test fun `bridge event stores uppercase action target expiration and unprocessed state`() {
        start()
        handler.enqueueBridgeEvent("banip", "192.0.2.1", reason, -1)
        jdbc { c -> c.createStatement().use { s -> s.executeQuery("SELECT * FROM bridge_events").use {
            assertTrue(it.next()); assertEquals("BANIP", it.getString("action"))
            assertEquals("192.0.2.1", it.getString("target")); assertEquals(reason, it.getString("reason"))
            assertEquals(-1L, it.getLong("endTime")); assertEquals(0, it.getInt("processed")); assertNull(it.getObject("processedAt"))
        } } }
    }

    @Test fun `schema creation is idempotent and rows persist across a new handler`() {
        start(); val id = add()
        handler.createTables(); assertEquals(1, handler.countAllPunishments())
        handler.closeConnection()
        val next = DatabaseHandler(plugin)
        try {
            next.openConnection(); next.createTables()
            assertTrue(next.isReady()); assertTrue(next.runHealthCheck().ok)
            assertEquals(id, next.getPunishments(uuid.toString()).single().id)
        } finally { next.closeConnection() }
    }

    @Test fun `schema failure leaves handler unready`() {
        sql("CREATE VIEW punishments AS SELECT 1 AS id")
        handler.openConnection()
        assertFails { handler.createTables() }
        assertFalse(handler.isReady())
    }

    @Test fun `failed insert returns null and never sends webhook`() {
        start()
        sql("CREATE TRIGGER reject_punishment BEFORE INSERT ON punishments BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        assertNull(handler.addPunishment("Target", uuid.toString(), reason, "Admin", "BAN", 1, -1))
        assertEquals(0, count("punishments")); verifyNoInteractions(plugin.discordWebhook)
    }

    @Test fun `reports reject duplicates enforce limit and count reports per suspect`() {
        start()
        val suspect = UUID.randomUUID()
        assertEquals(ReportSubmissionResult.Accepted(1), handler.submitReport(uuid, suspect, reason, 1))
        assertEquals(ReportSubmissionResult.ReporterAlreadyReportedSuspect, handler.submitReport(uuid, suspect, "duplicate", 1))
        assertEquals(ReportSubmissionResult.ReporterReachedOpenReportLimit, handler.submitReport(uuid, UUID.randomUUID(), reason, 1))
        assertEquals(ReportSubmissionResult.Accepted(2), handler.submitReport(UUID.randomUUID(), suspect, reason, 1))
        assertEquals(1, handler.countOpenReportsByReporter(uuid))
    }

    @Test fun `resolving report frees quota and keeps immutable decision metadata`() {
        start()
        val suspect = UUID.randomUUID()
        handler.submitReport(uuid, suspect, reason, 1)
        val report = handler.getReports().single()
        assertTrue(handler.resolveReport(report.id, "REJECTED", "Reviewer", reason))
        assertFalse(handler.resolveReport(report.id, "RESOLVED", "Other", "Overwrite"))
        assertFalse(handler.resolveReport(999, "RESOLVED", "Other", "Missing"))
        val closed = handler.getReports(closed = true).single()
        assertEquals("REJECTED", closed.status); assertEquals("Reviewer", closed.handledBy)
        assertEquals(reason, closed.note); assertNotNull(closed.handledAt)
        assertEquals(closed, handler.getReports(reportId = report.id).single())
        assertEquals(0, handler.countOpenReportsByReporter(uuid))
        assertEquals(ReportSubmissionResult.Accepted(1), handler.submitReport(uuid, suspect, reason, 1))
    }

    @Test fun `invalid resolution fields never close a report`() {
        start(); handler.addReport(uuid, UUID.randomUUID(), reason)
        val id = handler.getReports().single().id
        for ((status, note) in listOf("OPEN" to "Reason", "RESOLVED" to "ab", "REJECTED" to "x".repeat(256))) {
            assertFailsWith<IllegalArgumentException> { handler.resolveReport(id, status, "Admin", note) }
        }
        assertEquals(1, handler.getReports().size); assertEquals(0, count("report_resolutions"))
    }

    @Test fun `concurrent submissions on one handler accept one duplicate only`() {
        start(); val suspect = UUID.randomUUID()
        val executor = Executors.newFixedThreadPool(4)
        try {
            val results = executor.invokeAll((1..8).map { Callable { handler.submitReport(uuid, suspect, reason, 10) } })
                .map { it.get(10, TimeUnit.SECONDS) }
            assertEquals(1, results.count { it is ReportSubmissionResult.Accepted })
            assertEquals(7, results.count { it == ReportSubmissionResult.ReporterAlreadyReportedSuspect })
            assertEquals(1, count("reports"))
        } finally { executor.shutdownNow() }
    }

    @Test fun `invalid report UUID is skipped without dropping valid records`() {
        start(); handler.addReport(uuid, UUID.randomUUID(), reason)
        sql("INSERT INTO reports (player, suspect, reason) VALUES ('invalid', 'invalid', 'legacy')")
        assertEquals(1, handler.getReports().size)
    }

    @Test fun `deleting a closed report removes its resolution and reports missing IDs`() {
        start(); handler.addReport(uuid, UUID.randomUUID(), reason)
        val id = handler.getReports().single().id
        handler.resolveReport(id, "RESOLVED", "Admin", "Done")
        assertTrue(handler.deleteReport(id))
        assertFalse(handler.deleteReport(id))
        assertEquals(0, count("reports")); assertEquals(0, count("report_resolutions"))
    }

    @Test fun `failed report deletion rolls back its resolution`() {
        start(); handler.addReport(uuid, UUID.randomUUID(), reason)
        val id = handler.getReports().single().id
        handler.resolveReport(id, "RESOLVED", "Admin", "Done")
        sql("CREATE TRIGGER reject_report_delete BEFORE DELETE ON reports BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        assertFalse(handler.deleteReport(id))
        assertEquals(1, count("reports")); assertEquals(1, count("report_resolutions"))
    }

    @Test fun `report pagination has stable ID order when timestamps tie`() {
        start()
        repeat(5) { handler.addReport(uuid, UUID.randomUUID(), "Reason $it") }
        sql("UPDATE reports SET filedAt = '2026-01-01 12:00:00'")
        assertEquals(listOf(5, 4), handler.getReports(2, 0).map { it.id })
        assertEquals(listOf(3, 2), handler.getReports(2, 2).map { it.id })
    }

    @Test fun `report failures propagate or return explicit database error`() {
        start(); sql("DROP TABLE reports")
        assertFalse(handler.addReport(uuid, UUID.randomUUID(), reason))
        assertEquals(ReportSubmissionResult.DatabaseError, handler.submitReport(uuid, UUID.randomUUID(), reason, 1))
        assertFails { handler.getReports() }
    }

    @Test fun `invalid migration source and identical engine never schedule or change configuration`() {
        start()
        assertFalse(handler.migrateDatabase(DatabaseType.MYSQL, DatabaseType.SQLITE).get().success)
        assertFalse(handler.migrateDatabase(DatabaseType.SQLITE, DatabaseType.SQLITE).get().success)
        assertEquals("sqlite", config.getString("database.type"))
        verify(plugin.server, never()).scheduler
    }

    @Test fun `handler export and import restore generated IDs and Unicode cache`() {
        start(); val id = add(); handler.savePlayerCacheLine(reason)
        handler.exportDatabase()
        assertTrue(folder.resolve("dump/backup.sql").isFile)
        sql("DELETE FROM punishments"); sql("DELETE FROM playercache")
        handler.importDatabase()
        assertEquals(id, handler.getPunishments(uuid.toString()).single().id)
        assertEquals(listOf(reason), handler.getPlayerCacheLines())
        assertTrue(add() > id)
    }

    @Test fun `missing or invalid dump fails without changing existing data`() {
        start(); add()
        assertFails { handler.importDatabase() }
        folder.resolve("dump").mkdirs()
        folder.resolve("dump/backup.sql").writeText("INSERT INTO playercache (id,data) VALUES (1,'test'); DROP TABLE punishments;")
        assertFails { handler.importDatabase() }
        assertEquals(1, count("punishments")); assertEquals(0, count("playercache"))
    }
}
